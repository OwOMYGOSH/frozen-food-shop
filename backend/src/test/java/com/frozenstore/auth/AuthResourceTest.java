package com.frozenstore.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Auth API 整合測試
 *
 * @QuarkusTest：
 * - 啟動完整的 Quarkus 應用程式
 * - 透過 Dev Services 自動啟動 PostgreSQL 和 Redis 測試容器
 * - 每次測試使用乾淨的 DB（Flyway 自動執行 migration）
 *
 * 測試順序：@TestMethodOrder 確保 Register → Login → 重複註冊 的順序執行
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthResourceTest {

    private static final String BASE_PATH = "/api/auth";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NAME = "測試用戶";

    // ── Register ──

    @Test
    @Order(1)
    void register_success() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """.formatted(TEST_EMAIL, TEST_PASSWORD, TEST_NAME))
        .when()
            .post(BASE_PATH + "/register")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("email", equalTo(TEST_EMAIL))
            .body("name", equalTo(TEST_NAME))
            .body("role", equalTo("CUSTOMER"))
            .body("createdAt", notNullValue())
            // 確保密碼沒有出現在回應裡
            .body("$", not(hasKey("password")));
    }

    @Test
    @Order(2)
    void register_duplicate_email_returns_400() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "%s",
                    "password": "anotherPassword123",
                    "name": "另一個人"
                }
                """.formatted(TEST_EMAIL))
        .when()
            .post(BASE_PATH + "/register")
        .then()
            .statusCode(400)
            .body("error", equalTo("Bad Request"))
            .body("message", containsString("Email 已被註冊"));
    }

    @Test
    @Order(3)
    void register_invalid_email_returns_422() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "not-an-email",
                    "password": "password123",
                    "name": "測試"
                }
                """)
        .when()
            .post(BASE_PATH + "/register")
        .then()
            .statusCode(422)
            .body("error", equalTo("Validation Failed"))
            .body("message", containsString("email"));
    }

    @Test
    @Order(4)
    void register_short_password_returns_422() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "new@example.com",
                    "password": "short",
                    "name": "測試"
                }
                """)
        .when()
            .post(BASE_PATH + "/register")
        .then()
            .statusCode(422)
            .body("message", containsString("password"));
    }

    // ── Login ──

    @Test
    @Order(5)
    void login_success() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(TEST_EMAIL, TEST_PASSWORD))
        .when()
            .post(BASE_PATH + "/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .body("expiresIn", greaterThan(0))
            // Refresh Token 應在 Cookie，不在 body
            .body("$", not(hasKey("refreshToken")))
            // 確認有設定 Cookie
            .cookie(AuthService.REFRESH_COOKIE_NAME, notNullValue());
    }

    @Test
    @Order(6)
    void login_wrong_password_returns_401() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "%s",
                    "password": "wrongPassword"
                }
                """.formatted(TEST_EMAIL))
        .when()
            .post(BASE_PATH + "/login")
        .then()
            .statusCode(401);
    }

    @Test
    @Order(7)
    void login_nonexistent_email_returns_401() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "nobody@example.com",
                    "password": "password123"
                }
                """)
        .when()
            .post(BASE_PATH + "/login")
        .then()
            .statusCode(401);
    }

    // ── Logout ──

    @Test
    @Order(8)
    void logout_returns_204() {
        given()
        .when()
            .post(BASE_PATH + "/logout")
        .then()
            .statusCode(204);
    }
}
