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
 * ProductResource 整合測試
 *
 * V1__init.sql 種子資料有 5 個分類，id=1 是「海鮮」，測試用這個 categoryId。
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductResourceTest {

    private static final String BASE_PATH = "/api/products";

    // 記錄建立的商品 id，供後續測試使用
    private static Long createdProductId;

    // ── GET /api/products ──

    @Test
    @Order(1)
    void listProducts_returns_page() {
        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("content",       notNullValue())
            .body("page",          is(0))
            .body("size",          is(20))
            .body("totalElements", greaterThanOrEqualTo(0))
            .body("first",         is(true));
    }

    @Test
    @Order(2)
    void listProducts_with_sort_price_asc_returns_200() {
        given()
            .queryParam("sort", "price_asc")
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200);
    }

    @Test
    @Order(3)
    void listProducts_with_invalid_category_returns_404() {
        given()
            .queryParam("category", "nonexistent-slug")
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(404);
    }

    // ── POST /api/products ──

    @Test
    @Order(4)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createProduct_admin_returns_201() {
        String responseBody = given()
            .contentType("application/json")
            .body("""
                {
                  "categoryId": 1,
                  "name": "野生鮑魚",
                  "description": "新鮮急凍，肉質鮮嫩",
                  "price": 299.99,
                  "stockQty": 50,
                  "weightGrams": 300,
                  "isFrozen": true
                }
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(201)
            .body("id",           notNullValue())
            .body("name",         is("野生鮑魚"))
            .body("price",        is(299.99f))
            .body("categoryId",   is(1))
            .body("categoryName", containsString("海鮮"))
            .body("isActive",     is(true))
            .body("imageUrls",    hasSize(0))
            .extract().asString();

        // 取出 id 供後續測試使用
        createdProductId = io.restassured.path.json.JsonPath
            .from(responseBody).getLong("id");
    }

    @Test
    @Order(5)
    void createProduct_unauthenticated_returns_401() {
        given()
            .contentType("application/json")
            .body("""
                {"categoryId": 1, "name": "測試商品", "price": 100.00, "stockQty": 0, "isFrozen": true}
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(401);
    }

    @Test
    @Order(6)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createProduct_missing_required_fields_returns_422() {
        given()
            .contentType("application/json")
            .body("""
                {"name": "缺少 categoryId 和 price"}
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(422);
    }

    @Test
    @Order(7)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void createProduct_invalid_category_returns_400() {
        given()
            .contentType("application/json")
            .body("""
                {
                  "categoryId": 99999,
                  "name": "測試商品",
                  "price": 100.00,
                  "stockQty": 0,
                  "isFrozen": true
                }
                """)
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(400);
    }

    // ── GET /api/products/{id} ──

    @Test
    @Order(8)
    void getProduct_returns_200() {
        // 確保 createProduct test 已執行
        if (createdProductId == null) return;

        given()
        .when()
            .get(BASE_PATH + "/" + createdProductId)
        .then()
            .statusCode(200)
            .body("id",       is(createdProductId.intValue()))
            .body("name",     is("野生鮑魚"))
            .body("isActive", is(true));
    }

    @Test
    @Order(9)
    void getProduct_not_found_returns_404() {
        given()
        .when()
            .get(BASE_PATH + "/99999")
        .then()
            .statusCode(404);
    }

    // ── GET /api/products?category=seafood ──

    @Test
    @Order(10)
    void listProducts_filter_by_category_returns_correct_result() {
        if (createdProductId == null) return;

        given()
            .queryParam("category", "seafood")
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(1))
            .body("content[0].categoryName", containsString("海鮮"));
    }

    @Test
    @Order(11)
    void listProducts_filter_by_keyword_returns_correct_result() {
        if (createdProductId == null) return;

        given()
            .queryParam("keyword", "鮑魚")
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("totalElements", greaterThanOrEqualTo(1))
            .body("content[0].name", containsString("鮑魚"));
    }

    // ── PUT /api/products/{id} ──

    @Test
    @Order(12)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void updateProduct_admin_returns_200() {
        if (createdProductId == null) return;

        given()
            .contentType("application/json")
            .body("""
                {
                  "categoryId": 1,
                  "name": "野生鮑魚（特選）",
                  "description": "精選大顆，每顆 300g 以上",
                  "price": 349.99,
                  "stockQty": 30,
                  "weightGrams": 350,
                  "isFrozen": true
                }
                """)
        .when()
            .put(BASE_PATH + "/" + createdProductId)
        .then()
            .statusCode(200)
            .body("name",     is("野生鮑魚（特選）"))
            .body("price",    is(349.99f))
            .body("stockQty", is(30));
    }

    // ── DELETE /api/products/{id}（軟刪除）──

    @Test
    @Order(13)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deleteProduct_admin_returns_204() {
        if (createdProductId == null) return;

        given()
        .when()
            .delete(BASE_PATH + "/" + createdProductId)
        .then()
            .statusCode(204);
    }

    @Test
    @Order(14)
    void getDeletedProduct_returns_404() {
        // 軟刪除後，GET 應該回 404
        if (createdProductId == null) return;

        given()
        .when()
            .get(BASE_PATH + "/" + createdProductId)
        .then()
            .statusCode(404);
    }

    @Test
    @Order(15)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void deleteCategory_with_active_products_returns_400() {
        // 建一個有上架商品的分類，確認刪除時被擋下
        int catId = given()
            .contentType("application/json")
            .body("""
                {"name": "測試分類", "slug": "test-cat-with-product", "sortOrder": 0}
                """)
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201)
            .extract().path("id");

        given()
            .contentType("application/json")
            .body(String.format("""
                {
                  "categoryId": %d,
                  "name": "測試商品",
                  "price": 100.00,
                  "stockQty": 1,
                  "isFrozen": false
                }
                """, catId))
        .when()
            .post(BASE_PATH)
        .then()
            .statusCode(201);

        // 嘗試刪除有商品的分類，應該被擋下（400）
        given()
        .when()
            .delete("/api/categories/" + catId)
        .then()
            .statusCode(400)
            .body("message", containsString("商品"));
    }
}
