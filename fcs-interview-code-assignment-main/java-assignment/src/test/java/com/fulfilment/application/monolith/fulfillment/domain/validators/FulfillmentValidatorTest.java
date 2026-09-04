package com.fulfilment.application.monolith.fulfillment.domain.validators;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.fulfillment.domain.models.FulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FulfillmentValidatorTest {

  private final List<FulfillmentAssociation> stored = new ArrayList<>();

  private final FulfillmentStore fulfillmentStore =
      new FulfillmentStore() {
        @Override
        public boolean existsAssociation(Long storeId, Long productId, Long warehouseId) {
          return stored.stream()
              .anyMatch(
                  a ->
                      a.storeId.equals(storeId)
                          && a.productId.equals(productId)
                          && a.warehouseId.equals(warehouseId));
        }

        @Override
        public long countDistinctWarehousesForProductInStore(Long storeId, Long productId) {
          return stored.stream()
              .filter(a -> a.storeId.equals(storeId) && a.productId.equals(productId))
              .map(a -> a.warehouseId)
              .distinct()
              .count();
        }

        @Override
        public long countDistinctWarehousesForStore(Long storeId) {
          return stored.stream()
              .filter(a -> a.storeId.equals(storeId))
              .map(a -> a.warehouseId)
              .distinct()
              .count();
        }

        @Override
        public long countDistinctProductsForWarehouse(Long warehouseId) {
          return stored.stream()
              .filter(a -> a.warehouseId.equals(warehouseId))
              .map(a -> a.productId)
              .distinct()
              .count();
        }

        @Override
        public void create(FulfillmentAssociation association) {
          stored.add(association);
        }

        @Override
        public List<FulfillmentAssociation> findByStore(Long storeId) {
          return stored.stream().filter(a -> a.storeId.equals(storeId)).toList();
        }
      };

  private FulfillmentValidator validator;

  @BeforeEach
  public void setUp() {
    stored.clear();
    validator = new FulfillmentValidator(fulfillmentStore);
  }

  @Test
  public void testHappyPathDoesNotThrow() {
    assertDoesNotThrow(() -> validator.validate(new FulfillmentAssociation(1L, 1L, 1L)));
  }

  @Test
  public void testDuplicateAssociationIsRejected() {
    stored.add(new FulfillmentAssociation(1L, 1L, 1L));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validate(new FulfillmentAssociation(1L, 1L, 1L)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testMaxWarehousesPerProductPerStoreIsEnforced() {
    stored.add(new FulfillmentAssociation(1L, 1L, 1L));
    stored.add(new FulfillmentAssociation(1L, 1L, 2L));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validate(new FulfillmentAssociation(1L, 1L, 3L)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testMaxWarehousesPerStoreIsEnforced() {
    stored.add(new FulfillmentAssociation(1L, 1L, 1L));
    stored.add(new FulfillmentAssociation(1L, 2L, 2L));
    stored.add(new FulfillmentAssociation(1L, 3L, 3L));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validate(new FulfillmentAssociation(1L, 4L, 4L)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testMaxProductsPerWarehouseIsEnforced() {
    stored.add(new FulfillmentAssociation(1L, 1L, 1L));
    stored.add(new FulfillmentAssociation(2L, 2L, 1L));
    stored.add(new FulfillmentAssociation(3L, 3L, 1L));
    stored.add(new FulfillmentAssociation(4L, 4L, 1L));
    stored.add(new FulfillmentAssociation(5L, 5L, 1L));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validate(new FulfillmentAssociation(6L, 6L, 1L)));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
