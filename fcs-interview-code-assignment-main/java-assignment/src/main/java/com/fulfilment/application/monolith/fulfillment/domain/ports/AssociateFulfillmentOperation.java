package com.fulfilment.application.monolith.fulfillment.domain.ports;

import com.fulfilment.application.monolith.fulfillment.domain.models.FulfillmentAssociation;

public interface AssociateFulfillmentOperation {
  void associate(FulfillmentAssociation association);
}
