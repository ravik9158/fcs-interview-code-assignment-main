package com.fulfilment.application.monolith.fulfillment.adapters.database;

import com.fulfilment.application.monolith.fulfillment.domain.models.FulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class ProductFulfillmentRepository
    implements FulfillmentStore, PanacheRepository<DbProductFulfillment> {

  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  @Override
  public long countDistinctWarehousesForProductInStore(Long storeId, Long productId) {
    return find("store.id = ?1 and product.id = ?2", storeId, productId)
        .list()
        .stream()
        .map(pf -> pf.warehouse.id)
        .distinct()
        .count();
  }

  @Override
  public long countDistinctWarehousesForStore(Long storeId) {
    return find("store.id = ?1", storeId)
        .list()
        .stream()
        .map(pf -> pf.warehouse.id)
        .distinct()
        .count();
  }

  @Override
  public long countDistinctProductsForWarehouse(Long warehouseId) {
    return find("warehouse.id = ?1", warehouseId)
        .list()
        .stream()
        .map(pf -> pf.product.id)
        .distinct()
        .count();
  }

  @Override
  public boolean existsAssociation(Long storeId, Long productId, Long warehouseId) {
    return count(
            "store.id = ?1 and product.id = ?2 and warehouse.id = ?3",
            storeId,
            productId,
            warehouseId)
        > 0;
  }

  @Override
  public void create(FulfillmentAssociation association) {
    Store store = Store.findById(association.storeId);
    var product = productRepository.findById(association.productId);
    var warehouse = warehouseRepository.findById(association.warehouseId);
    persist(new DbProductFulfillment(store, product, warehouse));
  }

  @Override
  public List<FulfillmentAssociation> findByStore(Long storeId) {
    return find("store.id = ?1", storeId)
        .list()
        .stream()
        .map(pf -> new FulfillmentAssociation(pf.store.id, pf.product.id, pf.warehouse.id))
        .toList();
  }
}
