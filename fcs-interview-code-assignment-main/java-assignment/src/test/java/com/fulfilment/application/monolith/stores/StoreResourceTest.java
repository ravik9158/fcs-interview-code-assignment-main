package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  @Test
  public void testCrudStore() {
    final String path = "store";

    // List all, should have all 3 stores the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Create a new store, this exercises the legacy-sync-after-commit path for real:
    String newStore = "{\"name\":\"MALM\",\"quantityProductsInStock\":7}";
    Long newId =
        given()
            .contentType("application/json")
            .body(newStore)
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .body(containsString("MALM"))
            .extract()
            .jsonPath()
            .getLong("id");

    given().when().get(path + "/" + newId).then().statusCode(200).body(containsString("MALM"));

    // Update it:
    String updatedStore = "{\"name\":\"MALM-UPDATED\",\"quantityProductsInStock\":9}";
    given()
        .contentType("application/json")
        .body(updatedStore)
        .when()
        .put(path + "/" + newId)
        .then()
        .statusCode(200)
        .body(containsString("MALM-UPDATED"));

    // Patch it:
    String patchedStore = "{\"name\":\"MALM-PATCHED\",\"quantityProductsInStock\":11}";
    given()
        .contentType("application/json")
        .body(patchedStore)
        .when()
        .patch(path + "/" + newId)
        .then()
        .statusCode(200)
        .body(containsString("MALM-PATCHED"));

    // Delete it:
    given().when().delete(path + "/" + newId).then().statusCode(204);

    given().when().get(path + "/" + newId).then().statusCode(404);

    // The originally seeded stores are still untouched:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("TONSTAD"),
            containsString("KALLAX"),
            containsString("BESTÅ"),
            not(containsString("MALM-PATCHED")));
  }
}
