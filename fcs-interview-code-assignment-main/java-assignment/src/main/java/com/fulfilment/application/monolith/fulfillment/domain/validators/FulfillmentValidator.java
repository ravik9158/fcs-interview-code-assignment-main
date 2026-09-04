package com.fulfilment.application.monolith.fulfillment.domain.validators;

import com.fulfilment.application.monolith.fulfillment.domain.models.FulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class FulfillmentValidator {

  private static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  private static final int MAX_WAREHOUSES_PER_STORE = 3;
  private static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private final FulfillmentStore fulfillmentStore;

  public FulfillmentValidator(FulfillmentStore fulfillmentStore) {
    this.fulfillmentStore = fulfillmentStore;
  }

  public void validate(FulfillmentAssociation association) {
    if (fulfillmentStore.existsAssociation(
        association.storeId, association.productId, association.warehouseId)) {
      throw new WebApplicationException(
          "This product is already fulfilled by this warehouse for this store.", 400);
    }

    if (fulfillmentStore.countDistinctWarehousesForProductInStore(
            association.storeId, association.productId)
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "This product is already fulfilled by "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses in this store.",
          400);
    }

    if (fulfillmentStore.countDistinctWarehousesForStore(association.storeId)
        >= MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException(
          "This store is already fulfilled by " + MAX_WAREHOUSES_PER_STORE + " warehouses.", 400);
    }

    if (fulfillmentStore.countDistinctProductsForWarehouse(association.warehouseId)
        >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new WebApplicationException(
          "This warehouse already stocks " + MAX_PRODUCTS_PER_WAREHOUSE + " product types.", 400);
    }
  }
}
