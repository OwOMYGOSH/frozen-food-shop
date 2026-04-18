package com.frozenstore.auth;

import com.frozenstore.auth.dto.*;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.UUID;

/**
 * Auth 業務邏輯層
 *
 * 負責：Register、Login、Logout、Refresh Token
 * 不處理：HTTP 層細節（那是 AuthResource 的工作）
 * 不處理：輸入驗證（那是 Bean Validation 在 DTO 層的工作）
 *
 * 依賴注入清單：
 * - RedisDataSource：Refresh Token 的 key-value 儲存
 * - ConfigProperty：從 application.properties 讀設定值
 */
@ApplicationScoped
public class AuthService {

    // Refresh Token 存在 Redis 的 key 前綴
    private static final String REFRESH_KEY_PREFIX = "refresh_token:";
    // Refresh Token 有效期：7 天（秒）
    private static final long REFRESH_TOKEN_TTL = 7 * 24 * 3600L;
    // Cookie 名稱
    static final String REFRESH_COOKIE_NAME = "refresh_token";

    @Inject
    RedisDataSource redisDataSource;

    @ConfigProperty(name = "smallrye.jwt.new-token.lifespan", defaultValue = "3600")
    long jwtLifespanSeconds;

    // Redis String 操作的 helper
    // 為什麼用 @PostConstruct 而不是直接 @Inject ValueCommands：
    // ValueCommands 不是 CDI Bean，它是從 RedisDataSource 取出的物件，
    // 所以要在 Bean 初始化完成後（所有 @Inject 都注入好了）再呼叫 .value()
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands    = redisDataSource.key(String.class);
    }

    // ── Register ──────────────────────────────────────────────────────────

    /**
     * 新用戶註冊
     *
     * @throws IllegalArgumentException 如果 email 已被使用
     */
    @Transactional  // DB 操作必須在 transaction 裡
    public RegisterResponse register(RegisterRequest req) {
        // 1. 確認 email 唯一
        if (UserEntity.existsByEmail(req.email())) {
            throw new IllegalArgumentException("此 Email 已被註冊");
        }

        // 2. 建立用戶
        UserEntity user = new UserEntity();
        user.email    = req.email().toLowerCase().strip();
        user.password = BcryptUtil.bcryptHash(req.password()); // 永遠不存明文
        user.name     = req.name().strip();
        user.phone    = req.phone();

        // 3. 存入 DB（Active Record 的 persist()）
        user.persist();

        // 4. 回傳不含 password 的資料
        return new RegisterResponse(user.id, user.email, user.name, user.role, user.createdAt);
    }

    // ── Login ─────────────────────────────────────────────────────────────

    /**
     * 登入，成功後回傳帶有 Refresh Token Cookie 的 Response
     *
     * 為什麼回傳 Response 而不是 LoginResponse DTO？
     * 因為需要在 HTTP Response 上設定 Set-Cookie，
     * 這必須透過 JAX-RS 的 Response builder 來做。
     * AuthResource 直接把這個 Response 回傳給 client。
     *
     * @throws NotAuthorizedException 如果 email 不存在或密碼錯誤
     */
    public Response login(LoginRequest req) {
        // 1. 找用戶
        UserEntity user = UserEntity.findByEmail(req.email())
            .orElseThrow(() -> new NotAuthorizedException("Email 或密碼錯誤"));

        // 2. 確認帳號未停用
        if (!user.isActive) {
            throw new NotAuthorizedException("帳號已停用");
        }

        // 3. 驗證密碼（BCrypt compare，不是明文比對）
        if (!BcryptUtil.matches(req.password(), user.password)) {
            throw new NotAuthorizedException("Email 或密碼錯誤");
            // 注意：不單獨說「密碼錯誤」，避免讓攻擊者知道 email 存在
        }

        // 4. 簽發 Access Token（JWT）
        String accessToken = generateAccessToken(user);

        // 5. 產生 Refresh Token（UUID），存入 Redis
        String refreshToken = UUID.randomUUID().toString();
        valueCommands.setex(
            REFRESH_KEY_PREFIX + refreshToken,
            REFRESH_TOKEN_TTL,
            user.id.toString()
        );

        // 6. 把 Refresh Token 設為 HttpOnly Cookie
        NewCookie refreshCookie = new NewCookie.Builder(REFRESH_COOKIE_NAME)
            .value(refreshToken)
            .path("/api/auth")       // 只有 /api/auth 路徑才會帶上這個 Cookie
            .httpOnly(true)          // JS 無法讀取（防 XSS）
            .secure(false)           // 開發環境用 HTTP；生產環境改 true
            .maxAge((int) REFRESH_TOKEN_TTL)
            .build();

        return Response.ok(new LoginResponse(accessToken, "Bearer", jwtLifespanSeconds))
            .cookie(refreshCookie)
            .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────

    /**
     * 登出：刪除 Redis 裡的 Refresh Token，並清除 Cookie
     *
     * Access Token 無法「撤銷」（JWT 是無狀態的），
     * 所以登出後 Access Token 還有效直到它自然過期（1小時），
     * 這是 JWT 的特性，前端要在登出後立即清除本地儲存的 token。
     */
    public Response logout(String refreshToken) {
        if (refreshToken != null) {
            keyCommands.del(REFRESH_KEY_PREFIX + refreshToken);
        }

        // 回一個過期的 Cookie，讓瀏覽器刪除它
        NewCookie expiredCookie = new NewCookie.Builder(REFRESH_COOKIE_NAME)
            .value("")
            .path("/api/auth")
            .httpOnly(true)
            .secure(false)
            .maxAge(0)  // maxAge=0 代表立即刪除
            .build();

        return Response.noContent().cookie(expiredCookie).build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────

    /**
     * 用 Refresh Token 換新的 Access Token
     *
     * Token Rotation：
     * 每次 refresh 都刪舊的、建新的 Refresh Token，
     * 這樣即使舊 token 被竊取，只要正常用戶先用了，舊的就失效了。
     */
    public Response refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new NotAuthorizedException("缺少 Refresh Token");
        }

        // 1. 從 Redis 查 userId
        String userIdStr = valueCommands.get(REFRESH_KEY_PREFIX + refreshToken);
        if (userIdStr == null) {
            throw new NotAuthorizedException("Refresh Token 無效或已過期");
        }

        // 2. 查用戶
        Long userId = Long.parseLong(userIdStr);
        UserEntity user = UserEntity.findById(userId);
        if (user == null || !user.isActive) {
            throw new NotAuthorizedException("帳號不存在或已停用");
        }

        // 3. Token Rotation：刪舊的，建新的
        keyCommands.del(REFRESH_KEY_PREFIX + refreshToken);
        String newRefreshToken = UUID.randomUUID().toString();
        valueCommands.setex(
            REFRESH_KEY_PREFIX + newRefreshToken,
            REFRESH_TOKEN_TTL,
            userId.toString()
        );

        // 4. 新的 Access Token
        String newAccessToken = generateAccessToken(user);

        NewCookie newCookie = new NewCookie.Builder(REFRESH_COOKIE_NAME)
            .value(newRefreshToken)
            .path("/api/auth")
            .httpOnly(true)
            .secure(false)
            .maxAge((int) REFRESH_TOKEN_TTL)
            .build();

        return Response.ok(new LoginResponse(newAccessToken, "Bearer", jwtLifespanSeconds))
            .cookie(newCookie)
            .build();
    }

    // ── Private Helper ────────────────────────────────────────────────────

    private String generateAccessToken(UserEntity user) {
        return Jwt.issuer("frozen-food-shop")
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role)   // 前端讀取用
            .groups(user.role)          // @RolesAllowed 讀 groups claim
            .expiresIn(Duration.ofSeconds(jwtLifespanSeconds))
            .sign();
    }
}
