package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException(
          "A warehouse with business unit code "
              + warehouse.businessUnitCode
              + " already exists.",
          400);
    }

    Location location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new WebApplicationException("Unknown location: " + warehouse.location, 400);
    }

    if (warehouse.capacity == null || warehouse.stock == null || warehouse.capacity < warehouse.stock) {
      throw new WebApplicationException("Warehouse capacity must be at least its stock.", 400);
    }

    List<Warehouse> activeAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(warehouse.location) && w.archivedAt == null)
            .toList();

    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Location " + location.identification + " has reached its maximum number of warehouses.",
          400);
    }

    int usedCapacity = activeAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (usedCapacity + warehouse.capacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Location " + location.identification + " does not have enough remaining capacity.",
          400);
    }

    warehouse.createdAt = LocalDateTime.now();

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }
}
