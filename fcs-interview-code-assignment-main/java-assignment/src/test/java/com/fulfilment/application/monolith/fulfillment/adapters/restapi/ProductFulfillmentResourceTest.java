package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

// Stores and products are created fresh per test (cheap, no Location quota to worry about).
// Warehouses reuse the seeded rows (ids 1/2/3) where possible, since Location has tight quotas
// (e.g. TILBURG-001 allows only 1 warehouse total) and spinning up many fresh ones across tests
// would fight over that shared capacity. Warehouses are never hard-deleted by any test (only
// archived, a soft update), so pointing fulfilment rows at seeded warehouses is safe - unlike
// seeded products, which ProductEndpointTest hard-deletes.
@QuarkusTest
public class ProductFulfillmentResourceTest {

  private static final AtomicInteger COUNTER = new AtomicInteger();

  private String associationBody(long storeId, long productId, long warehouseId) {
    return "{\"storeId\":"
        + storeId
        + ",\"productId\":"
        + productId
        + ",\"warehouseId\":"
        + warehouseId
        + "}";
  }

  private long createStore() {
    String body = "{\"name\":\"FUL-STORE-" + COUNTER.incrementAndGet() + "\"}";
    return given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private long createProduct() {
    String body = "{\"name\":\"FUL-PRODUCT-" + COUNTER.incrementAndGet() + "\"}";
    return given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private long createWarehouse(String location) {
    String body =
        "{\"businessUnitCode\":\"MWH.FUL"
            + COUNTER.incrementAndGet()
            + "\",\"location\":\""
            + location
            + "\",\"capacity\":10,\"stock\":0}";
    return given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("warehouse")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  @Test
  public void testAssociateHappyPath() {
    long store = createStore();
    long product = createProduct();

    given()
        .contentType("application/json")
        .body(associationBody(store, product, 1)) // seeded warehouse MWH.001
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201);
  }

  @Test
  public void testDuplicateAssociationIsRejected() {
    long store = createStore();
    long product = createProduct();

    given()
        .contentType("application/json")
        .body(associationBody(store, product, 1))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(associationBody(store, product, 1))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(400);
  }

  @Test
  public void testMaxWarehousesPerProductPerStoreIsEnforced() {
    long store = createStore();
    long product = createProduct();

    // Seeded warehouses 1, 2, 3 - 3 distinct warehouses for the same product+store.
    given()
        .contentType("application/json")
        .body(associationBody(store, product, 1))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(associationBody(store, product, 2))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201);

    // 3rd distinct warehouse for the same product+store must be rejected.
    given()
        .contentType("application/json")
        .body(associationBody(store, product, 3))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(400);
  }

  @Test
  public void testMaxWarehousesPerStoreIsEnforced() {
    long store = createStore();
    long product1 = createProduct();
    long product2 = createProduct();
    long product3 = createProduct();

    // 3 distinct warehouses for this store, via 3 different products - at the limit.
    given()
        .contentType("application/json")
        .body(associationBody(store, product1, 1))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(associationBody(store, product2, 2))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201);

    given()
        .contentType("application/json")
        .body(associationBody(store, product3, 3))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(201);

    // A 4th distinct warehouse for the same store must be rejected, even for a fresh
    // product+warehouse pair. EINDHOVEN-001 has spare capacity and isn't used elsewhere.
    long warehouse4 = createWarehouse("EINDHOVEN-001");
    given()
        .contentType("application/json")
        .body(associationBody(store, product1, warehouse4))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(400);
  }

  @Test
  public void testMaxProductsPerWarehouseIsEnforced() {
    long store = createStore();
    // Dedicated fresh warehouse so other tests' associations don't inflate its product count.
    long warehouse = createWarehouse("VETSBY-001");

    // 5 distinct products for this warehouse - at the limit.
    for (int i = 0; i < 5; i++) {
      long product = createProduct();
      given()
          .contentType("application/json")
          .body(associationBody(store, product, warehouse))
          .when()
          .post("fulfillment")
          .then()
          .statusCode(201);
    }

    // 6th distinct product for the same warehouse must be rejected.
    long sixthProduct = createProduct();
    given()
        .contentType("application/json")
        .body(associationBody(store, sixthProduct, warehouse))
        .when()
        .post("fulfillment")
        .then()
        .statusCode(400);
  }
}
