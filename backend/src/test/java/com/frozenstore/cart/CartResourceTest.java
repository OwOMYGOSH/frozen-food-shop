package com.frozenstore.cart;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * CartResource 整合測試
 *
 * @TestSecurity(user = "1", roles = "CUSTOMER")
 * → user = "1" 對應 JWT 的 sub 欄位，CartResource 用 getName() 取出當 userId
 *
 * 測試類自己建立所需的商品（Order 0），不依賴其他測試類的執行順序
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CartResourceTest {

    private static final String BASE_PATH = "/api/cart";
    private static int testProductId;

    // ── 建立測試用商品（讓後面的測試使用）──

    @Test
    @Order(0)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void setup_createTestProduct() {
        testProductId = given()
            .contentType("application/json")
            .body("""
                {
                  "categoryId": 1,
                  "name": "購物車測試商品",
                  "price": 199.00,
                  "stockQty": 100,
                  "isFrozen": true
                }
                """)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().path("id");
    }

    // ── GET /api/cart（未登入）──

    @Test
    @Order(1)
    void getCart_unauthenticated_returns_401() {
        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(401);
    }

    // ── GET /api/cart（空購物車）──

    @Test
    @Order(2)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void getCart_empty_returns_empty_list() {
        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("items",      hasSize(0))
            .body("totalItems", is(0));
    }

    // ── POST /api/cart/items ──

    @Test
    @Order(3)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void addItem_returns_201() {
        given()
            .contentType("application/json")
            .body(String.format("""
                { "productId": %d, "quantity": 2 }
                """, testProductId))
        .when()
            .post(BASE_PATH + "/items")
        .then()
            .statusCode(201);
    }

    @Test
    @Order(4)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void getCart_after_add_returns_items() {
        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("items",              hasSize(1))
            .body("items[0].productId", is(testProductId))
            .body("items[0].quantity",  is(2))
            .body("totalItems",         is(2));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void addItem_same_product_accumulates_quantity() {
        given()
            .contentType("application/json")
            .body(String.format("""
                { "productId": %d, "quantity": 3 }
                """, testProductId))
        .when()
            .post(BASE_PATH + "/items")
        .then()
            .statusCode(201);

        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("items[0].quantity", is(5));
    }

    // ── PUT /api/cart/items/{productId} ──

    @Test
    @Order(6)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void updateItem_changes_quantity() {
        given()
            .contentType("application/json")
            .body("""
                { "quantity": 1 }
                """)
        .when()
            .put(BASE_PATH + "/items/" + testProductId)
        .then()
            .statusCode(204);

        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("items[0].quantity", is(1));
    }

    @Test
    @Order(7)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void updateItem_quantity_zero_removes_item() {
        given()
            .contentType("application/json")
            .body("""
                { "quantity": 0 }
                """)
        .when()
            .put(BASE_PATH + "/items/" + testProductId)
        .then()
            .statusCode(204);

        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("items", hasSize(0));
    }

    // ── DELETE /api/cart/items/{productId} ──

    @Test
    @Order(8)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void removeItem_returns_204() {
        given()
            .contentType("application/json")
            .body(String.format("""
                { "productId": %d, "quantity": 2 }
                """, testProductId))
        .when()
            .post(BASE_PATH + "/items")
        .then()
            .statusCode(201);

        given()
        .when()
            .delete(BASE_PATH + "/items/" + testProductId)
        .then()
            .statusCode(204);

        given()
        .when()
            .get(BASE_PATH)
        .then()
            .statusCode(200)
            .body("items", hasSize(0));
    }
}
