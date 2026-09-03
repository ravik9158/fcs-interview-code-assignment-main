package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse existing = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new WebApplicationException(
          "No active warehouse with business unit code " + newWarehouse.businessUnitCode, 404);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new WebApplicationException("Unknown location: " + newWarehouse.location, 400);
    }

    if (newWarehouse.capacity == null
        || newWarehouse.stock == null
        || newWarehouse.capacity < newWarehouse.stock) {
      throw new WebApplicationException("Warehouse capacity must be at least its stock.", 400);
    }

    // Capacity accommodation: the new warehouse must be able to hold the stock of the one it
    // is replacing.
    if (newWarehouse.capacity < existing.stock) {
      throw new WebApplicationException(
          "New warehouse capacity cannot hold the stock of the warehouse being replaced.", 400);
    }

    // Stock matching: the new warehouse must start with exactly the same stock as the old one.
    if (!newWarehouse.stock.equals(existing.stock)) {
      throw new WebApplicationException(
          "New warehouse stock must match the stock of the warehouse being replaced.", 400);
    }

    List<Warehouse> activeAtLocation =
        warehouseStore.getAll().stream()
            .filter(
                w ->
                    w.location.equals(newWarehouse.location)
                        && w.archivedAt == null
                        && !w.businessUnitCode.equals(existing.businessUnitCode))
            .toList();

    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Location " + location.identification + " has reached its maximum number of warehouses.",
          400);
    }

    int usedCapacity = activeAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (usedCapacity + newWarehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Location " + location.identification + " does not have enough remaining capacity.",
          400);
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    newWarehouse.id = null;
    newWarehouse.createdAt = LocalDateTime.now();
    warehouseStore.create(newWarehouse);
  }
}
