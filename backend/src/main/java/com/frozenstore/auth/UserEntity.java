package com.frozenstore.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * User 資料庫實體（Active Record 模式）
 *
 * Active Record 模式：查詢方法直接寫在 Entity 上（findByEmail、existsByEmail），
 * 不需要額外的 Repository 類別，適合這種中小型專案。
 *
 * 欄位設計原則：
 * - password 允許 null（OAuth 用戶沒有密碼）
 * - role 預設 CUSTOMER，只有手動設定才能變成 ADMIN
 * - is_active 用於帳號停用，不是刪除（軟刪除的一種形式）
 */
@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 255)
    public String email;

    /** BCrypt hash，OAuth 用戶為 null */
    @Column(length = 255)
    public String password;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(length = 20)
    public String phone;

    /** CUSTOMER | ADMIN */
    @Column(nullable = false, length = 20)
    public String role = "CUSTOMER";

    @Column(name = "is_active", nullable = false)
    public boolean isActive = true;

    @Column(name = "oauth_provider", length = 20)
    public String oauthProvider;

    @Column(name = "oauth_subject", length = 255)
    public String oauthSubject;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // ── 查詢方法（Active Record 風格）──

    /** 依 email 找用戶，登入和重複信箱檢查都用這個 */
    public static Optional<UserEntity> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    /** 快速確認 email 是否已被註冊，比 findByEmail 省一點資源 */
    public static boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
}
