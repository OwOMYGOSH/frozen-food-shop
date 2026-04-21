package com.frozenstore.order;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * OrderResource 整合測試
 *
 * 測試類自己建立所需商品（Order 0），不依賴其他測試類的執行順序
 * userId=1 透過 @TestSecurity(user = "1") 模擬，對應 JWT sub 欄位
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderResourceTest {

    private static final String CART_PATH  = "/api/cart";
    private static final String ORDER_PATH = "/api/orders";
    private static int testProductId;

    // ── 建立測試用商品 ──

    @Test
    @Order(0)
    @TestSecurity(user = "admin", roles = "ADMIN")
    void setup_createTestProduct() {
        testProductId = given()
            .contentType("application/json")
            .body("""
                {
                  "categoryId": 1,
                  "name": "訂單測試商品",
                  "price": 299.00,
                  "stockQty": 50,
                  "isFrozen": true
                }
                """)
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().path("id");
    }

    // ── 空購物車結帳 ──

    @Test
    @Order(1)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void createOrder_empty_cart_returns_400() {
        given()
            .contentType("application/json")
            .body("""
                {
                  "shippingName":    "王小明",
                  "shippingPhone":   "0912345678",
                  "shippingAddress": "台北市信義區測試路1號"
                }
                """)
        .when()
            .post(ORDER_PATH)
        .then()
            .statusCode(400);
    }

    // ── 正常結帳流程 ──

    @Test
    @Order(2)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void createOrder_success_returns_201() {
        given()
            .contentType("application/json")
            .body(String.format("""
                { "productId": %d, "quantity": 1 }
                """, testProductId))
        .when()
            .post(CART_PATH + "/items")
        .then()
            .statusCode(201);

        given()
            .contentType("application/json")
            .body("""
                {
                  "shippingName":    "王小明",
                  "shippingPhone":   "0912345678",
                  "shippingAddress": "台北市信義區測試路1號"
                }
                """)
        .when()
            .post(ORDER_PATH)
        .then()
            .statusCode(201)
            .body("id",                  notNullValue())
            .body("status",              is("PENDING_PAYMENT"))
            .body("items",               hasSize(1))
            .body("items[0].productId",  is(testProductId))
            .body("shippingName",        is("王小明"))
            .body("totalAmount",         notNullValue());
    }

    @Test
    @Order(3)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void createOrder_clears_cart_after_success() {
        given()
        .when()
            .get(CART_PATH)
        .then()
            .statusCode(200)
            .body("items", hasSize(0));
    }

    // ── 查詢訂單 ──

    @Test
    @Order(4)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void listMyOrders_returns_created_order() {
        given()
        .when()
            .get(ORDER_PATH)
        .then()
            .statusCode(200)
            .body("size()",     greaterThanOrEqualTo(1))
            .body("[0].id",     notNullValue())
            .body("[0].status", is("PENDING_PAYMENT"))
            .body("[0].items",  hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void getOrder_returns_detail() {
        int orderId = given()
        .when()
            .get(ORDER_PATH)
        .then()
            .statusCode(200)
            .extract().path("[0].id");

        given()
        .when()
            .get(ORDER_PATH + "/" + orderId)
        .then()
            .statusCode(200)
            .body("id",           is(orderId))
            .body("items",        hasSize(greaterThanOrEqualTo(1)))
            .body("shippingName", is("王小明"));
    }

    @Test
    @Order(6)
    @TestSecurity(user = "1", roles = "CUSTOMER")
    void getOrder_not_found_returns_404() {
        given()
        .when()
            .get(ORDER_PATH + "/99999")
        .then()
            .statusCode(404);
    }

    // ── 未登入 ──

    @Test
    @Order(7)
    void createOrder_unauthenticated_returns_401() {
        given()
            .contentType("application/json")
            .body("""
                {
                  "shippingName":    "王小明",
                  "shippingPhone":   "0912345678",
                  "shippingAddress": "台北市信義區測試路1號"
                }
                """)
        .when()
            .post(ORDER_PATH)
        .then()
            .statusCode(401);
    }
}
