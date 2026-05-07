package com.frozenstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 設定
 *
 * 對應 Quarkus：Quarkus Security 大多靠 @RolesAllowed 這類 annotation 隱式啟動。
 * Spring Security 必須顯式建一條 SecurityFilterChain 來宣告「誰能走、誰要擋」。
 *
 * 這個 FilterChain 的責任：
 *   1. 關閉 session（純 JWT，無狀態）
 *   2. 關掉 CSRF（無 session 不需要 CSRF token；但要開 CORS）
 *   3. 定義路由權限（白名單 / 要登入 / 要特定角色）
 *   4. 設定 JWT resource server（告訴 Spring 怎麼驗 Bearer token）
 *   5. 把 JWT 裡的 "role" claim 轉成 Spring Security 認得的 GrantedAuthority
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 無狀態 API，不走 session
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 沒有 session 就不需要 CSRF 保護
            .csrf(AbstractHttpConfigurer::disable)
            // 啟用 CORS，使用下面定義的 corsConfigurationSource()
            .cors(cors -> {})
            // 路由權限
            .authorizeHttpRequests(auth -> auth
                // 白名單：登入/註冊/健康檢查/商品瀏覽不需登入
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // 其他全部要登入才能走
                .anyRequest().authenticated()
            )
            // JWT Resource Server：告訴 Spring 用 publicKey.pem 驗 token 簽章
            // publicKey 的位置在 application.yml 的
            // spring.security.oauth2.resourceserver.jwt.public-key-location
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * 把 JWT 裡的 claim 轉成 Spring Security 認得的權限
     *
     * 我們的 JWT 格式：{ "sub": "1", "email": "...", "role": "CUSTOMER", ... }
     * Spring Security 預設讀 "scope" / "scp" claim，所以要自訂成讀 "role"。
     *
     * 輸出的 authority 會加上 "ROLE_" 前綴 → "ROLE_CUSTOMER"
     * 之後在 Controller 上可寫 @PreAuthorize("hasRole('CUSTOMER')")
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        // principal name = JWT 的 sub 欄位（也就是 userId）
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * BCrypt 密碼雜湊
     *
     * 對應 Quarkus 的 BcryptUtil.bcryptHash() / BcryptUtil.matches()。
     * Spring Boot 的做法是注入一個 PasswordEncoder bean，在 AuthService 裡用
     * passwordEncoder.encode(raw) 和 passwordEncoder.matches(raw, hash)。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 設定
     *
     * 對應 Quarkus 的 quarkus.http.cors.* properties。
     * dev 環境允許 http://localhost:4200（Angular dev server）。
     * prod 要從環境變數讀，這裡先寫死，之後改 @ConfigurationProperties。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);   // 允許帶 Cookie（refresh token）

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
