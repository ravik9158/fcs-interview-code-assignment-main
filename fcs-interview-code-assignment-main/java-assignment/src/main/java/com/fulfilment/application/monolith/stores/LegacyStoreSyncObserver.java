package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

// Reacts to Store changes only after the transaction that made them has committed, so the
// legacy system never observes a Store that failed to persist.
@ApplicationScoped
public class LegacyStoreSyncObserver {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
  }

  public void onStoreUpdated(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
  }
}
