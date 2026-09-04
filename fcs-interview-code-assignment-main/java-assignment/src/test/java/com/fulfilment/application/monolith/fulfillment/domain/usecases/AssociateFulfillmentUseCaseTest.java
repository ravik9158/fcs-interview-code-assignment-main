package com.fulfilment.application.monolith.fulfillment.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.fulfillment.domain.models.FulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.fulfillment.domain.validators.FulfillmentValidator;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssociateFulfillmentUseCaseTest {

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
          return 0;
        }

        @Override
        public long countDistinctWarehousesForStore(Long storeId) {
          return 0;
        }

        @Override
        public long countDistinctProductsForWarehouse(Long warehouseId) {
          return 0;
        }

        @Override
        public void create(FulfillmentAssociation association) {
          stored.add(association);
        }

        @Override
        public List<FulfillmentAssociation> findByStore(Long storeId) {
          return stored;
        }
      };

  private AssociateFulfillmentUseCase useCase;

  @BeforeEach
  public void setUp() {
    stored.clear();
    useCase = new AssociateFulfillmentUseCase(fulfillmentStore, new FulfillmentValidator(fulfillmentStore));
  }

  @Test
  public void testAssociateCallsValidatorThenPersists() {
    useCase.associate(new FulfillmentAssociation(1L, 1L, 1L));

    assertEquals(1, stored.size());
    assertEquals(1L, stored.get(0).storeId);
  }

  @Test
  public void testAssociateRejectedByValidatorIsNotPersisted() {
    stored.add(new FulfillmentAssociation(1L, 1L, 1L));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.associate(new FulfillmentAssociation(1L, 1L, 1L)));
    assertEquals(400, ex.getResponse().getStatus());
    assertEquals(1, stored.size()); // duplicate wasn't persisted
  }
}
