package com.fulfilment.application.monolith.fulfillment.domain.models;

public class FulfillmentAssociation {

  public Long storeId;

  public Long productId;

  public Long warehouseId;

  public FulfillmentAssociation() {}

  public FulfillmentAssociation(Long storeId, Long productId, Long warehouseId) {
    this.storeId = storeId;
    this.productId = productId;
    this.warehouseId = warehouseId;
  }
}
