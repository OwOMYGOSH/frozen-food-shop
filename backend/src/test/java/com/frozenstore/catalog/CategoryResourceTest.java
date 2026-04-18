package com.frozenstore.catalog;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * CategoryResource 整合測試
 *
 * V1__init.sql 種子資料有 5 筆分類（海鮮、肉類、蔬菜、點心甜食、熟食便當），
 * 測試開始時 DB 已有這 5 筆。
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryResourceTest {

    private static final String BASE_PATH = "/api/categories";

    // ── GET /api/categories ──

    @Test
    @Order(1)
    void listCategories_returns_seeded_data() {
        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(5))
            .body("[0].id",   notNullValue())
            .body("[0].name", notNullValue())
            .body("[0].slug", notNullValue());
    }

    // ── POST /api/categories ──

    @Test
    @Order(2)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createCategory_admin_returns_201() {
        given()
            .contentType("application/json")
            .body("""
                {
                  "name": "飲料",
                  "slug": "drinks",
                  "sortOrder": 10
                }
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(201)
            .body("id",        notNullValue())
            .body("name",      is("飲料"))
            .body("slug",      is("drinks"))
            .body("parentId",  nullValue())
            .body("sortOrder", is(10));
    }

    @Test
    @Order(3)
    void createCategory_unauthenticated_returns_401() {
        given()
            .contentType("application/json")
            .body("""
                {"name": "飲料", "slug": "drinks", "sortOrder": 0}
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(401);
    }

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createCategory_duplicate_slug_returns_400() {
        given()
            .contentType("application/json")
            .body("""
                {"name": "海鮮2", "slug": "seafood", "sortOrder": 0}
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(400)
            .body("message", containsString("已被使用"));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createCategory_invalid_slug_returns_422() {
        given()
            .contentType("application/json")
            .body("""
                {"name": "測試", "slug": "UPPERCASE", "sortOrder": 0}
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(422);
    }

    // ── PUT /api/categories/{id} ──

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateCategory_admin_returns_200() {
        // 更新種子分類 id=1（海鮮）的 sortOrder
        given()
            .contentType("application/json")
            .body("""
                {"name": "海鮮類", "slug": "seafood", "sortOrder": 99}
                """)
        .when()
            .put(BASE_PATH + "/1")
        .then()
            .statusCode(200)
            .body("name",      is("海鮮類"))
            .body("sortOrder", is(99));
    }

    @Test
    @Order(7)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateCategory_not_found_returns_404() {
        given()
            .contentType("application/json")
            .body("""
                {"name": "不存在", "slug": "not-exist", "sortOrder": 0}
                """)
        .when()
            .put(BASE_PATH + "/99999")
        .then()
            .statusCode(404);
    }

    // ── DELETE /api/categories/{id} ──

    @Test
    @Order(8)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deleteCategory_not_found_returns_404() {
        given()
        .when()
            .delete(BASE_PATH + "/99999")
        .then()
            .statusCode(404);
    }

    @Test
    @Order(9)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deleteCategory_with_no_products_returns_204() {
        // 先建一個新分類再刪掉（不會有商品）
        int newId = given()
            .contentType("application/json")
            .body("""
                {"name": "暫時分類", "slug": "temp-cat", "sortOrder": 0}
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
        .when()
            .delete(BASE_PATH + "/" + newId)
        .then()
            .statusCode(204);
    }
}
