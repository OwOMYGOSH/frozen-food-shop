package com.frozenstore.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * 簽發 Access Token（JWT）
 *
 * 對應 Quarkus 的 io.smallrye.jwt.build.Jwt.issuer(...).sign()。
 * Spring 本身只有「驗」JWT 的功能（OAuth2 Resource Server），
 * 「簽」要我們自己用 nimbus-jose-jwt 做。
 *
 * 流程：
 *   1. 啟動時讀 privateKey.pem → 解析成 RSAPrivateKey
 *   2. issueAccessToken(userId, email, role) 組 claims → 用 private key 簽
 *   3. 用戶拿這個 token 打 API 時，Spring Security 自動用 public key 驗簽
 */
@Service
public class JwtService {

    private final Resource privateKeyResource;
    private final String issuer;
    private final long lifespanSeconds;

    private RSAPrivateKey privateKey;

    public JwtService(
        @Value("${app.jwt.private-key-location}") Resource privateKeyResource,
        @Value("${app.jwt.issuer}") String issuer,
        @Value("${app.jwt.access-token-lifespan-seconds}") long lifespanSeconds
    ) {
        this.privateKeyResource = privateKeyResource;
        this.issuer = issuer;
        this.lifespanSeconds = lifespanSeconds;
    }

    @PostConstruct
    void init() throws IOException {
        // privateKey.pem 格式範例：
        //   -----BEGIN PRIVATE KEY-----
        //   MIIEv...
        //   -----END PRIVATE KEY-----
        // 要把頭尾和換行拿掉，Base64 decode 才能給 KeyFactory 用。
        String pem = new String(privateKeyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s+", "");
        byte[] decoded = Base64.getDecoder().decode(base64);

        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            this.privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("無法載入 JWT private key", e);
        }
    }

    /**
     * 簽一個 Access Token
     *
     * @param userId 放在 sub claim
     * @param email  放在 email claim
     * @param role   放在 role claim（SecurityConfig 會轉成 ROLE_{role} authority）
     */
    public String issueAccessToken(Long userId, String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(Duration.ofSeconds(lifespanSeconds));

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(String.valueOf(userId))
            .claim("email", email)
            .claim("role", role)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(exp))
            .build();

        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).build(),
            claims
        );

        try {
            jwt.sign(new RSASSASigner(privateKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("簽 JWT 失敗", e);
        }

        return jwt.serialize();
    }

    public long getLifespanSeconds() {
        return lifespanSeconds;
    }
}
