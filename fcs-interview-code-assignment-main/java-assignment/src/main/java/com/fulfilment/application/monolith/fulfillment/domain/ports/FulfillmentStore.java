package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.FulfillmentAssociation;
import java.util.List;

public interface FulfillmentStore {

  boolean existsAssociation(Long storeId, Long productId, Long warehouseId);

  long countDistinctWarehousesForProductInStore(Long storeId, Long productId);

  long countDistinctWarehousesForStore(Long storeId);

  long countDistinctProductsForWarehouse(Long warehouseId);

  void create(FulfillmentAssociation association);

  List<FulfillmentAssociation> findByStore(Long storeId);
}
