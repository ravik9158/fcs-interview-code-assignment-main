package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Associates a Warehouse as a fulfilment unit of a Product for a Store. Enforced quotas (max 2
// warehouses per product per store, max 3 warehouses per store, max 5 products per warehouse)
// live in ProductFulfillmentResource, not here - this is a plain association record.
@Entity
@Table(
    name = "product_fulfillment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"store_id", "product_id", "warehouse_id"}))
public class ProductFulfillment {

  @Id @GeneratedValue public Long id;

  @ManyToOne public Store store;

  @ManyToOne public Product product;

  @ManyToOne public DbWarehouse warehouse;

  public ProductFulfillment() {}

  public ProductFulfillment(Store store, Product product, DbWarehouse warehouse) {
    this.store = store;
    this.product = product;
    this.warehouse = warehouse;
  }
}
