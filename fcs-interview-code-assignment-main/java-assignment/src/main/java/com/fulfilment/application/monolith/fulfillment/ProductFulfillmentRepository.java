package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductFulfillmentRepository implements PanacheRepository<ProductFulfillment> {

  public long countDistinctWarehousesForProductInStore(Long storeId, Long productId) {
    return find("store.id = ?1 and product.id = ?2", storeId, productId)
        .list()
        .stream()
        .map(pf -> pf.warehouse.id)
        .distinct()
        .count();
  }

  public long countDistinctWarehousesForStore(Long storeId) {
    return find("store.id = ?1", storeId)
        .list()
        .stream()
        .map(pf -> pf.warehouse.id)
        .distinct()
        .count();
  }

  public long countDistinctProductsForWarehouse(Long warehouseId) {
    return find("warehouse.id = ?1", warehouseId)
        .list()
        .stream()
        .map(pf -> pf.product.id)
        .distinct()
        .count();
  }

  public boolean existsAssociation(Long storeId, Long productId, Long warehouseId) {
    return count(
            "store.id = ?1 and product.id = ?2 and warehouse.id = ?3",
            storeId,
            productId,
            warehouseId)
        > 0;
  }
}
