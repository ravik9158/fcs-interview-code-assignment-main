package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

// Tests here share one DB instance with no per-test rollback, and later tests mutate seeded
// warehouses (archive, replace) - pin an explicit order so earlier assertions aren't affected
// by later tests' side effects.
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WarehouseEndpointTest {

  @Test
  @Order(1)
  public void testSimpleListWarehouses() {

    final String path = "warehouse";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  @Order(2)
  public void testCreateDuplicateBusinessUnitCodeIsRejected() {
    final String path = "warehouse";

    String duplicate =
        "{\"businessUnitCode\":\"MWH.001\",\"location\":\"HELMOND-001\",\"capacity\":20,\"stock\":5}";

    given()
        .contentType("application/json")
        .body(duplicate)
        .when()
        .post(path)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(3)
  public void testCreateAndGetWarehouse() {
    final String path = "warehouse";

    String newWarehouse =
        "{\"businessUnitCode\":\"MWH.900\",\"location\":\"HELMOND-001\",\"capacity\":20,\"stock\":5}";

    String id =
        given()
            .contentType("application/json")
            .body(newWarehouse)
            .when()
            .post(path)
            .then()
            .statusCode(200)
            .body(containsString("MWH.900"))
            .extract()
            .path("id")
            .toString();

    given()
        .when()
        .get(path + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("MWH.900"), containsString("HELMOND-001"));
  }

  @Test
  @Order(4)
  public void testReplaceWarehouse() {
    final String path = "warehouse";

    String replacement =
        "{\"businessUnitCode\":\"MWH.012\",\"location\":\"AMSTERDAM-001\",\"capacity\":80,\"stock\":5}";

    given()
        .contentType("application/json")
        .body(replacement)
        .when()
        .post(path + "/MWH.012/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.012"), containsString("\"capacity\":80"));

    given().when().get(path).then().statusCode(200).body(containsString("\"capacity\":80"));
  }

  @Test
  @Order(5)
  public void testSimpleCheckingArchivingWarehouses() {

    final String path = "warehouse";

    // List all, should still have all 3 originally seeded warehouses and their locations:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"),
            containsString("ZWOLLE-001"),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));

    // Archive the ZWOLLE-001:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, ZWOLLE-001 should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(
            not(containsString("ZWOLLE-001")),
            containsString("AMSTERDAM-001"),
            containsString("TILBURG-001"));
  }
}
