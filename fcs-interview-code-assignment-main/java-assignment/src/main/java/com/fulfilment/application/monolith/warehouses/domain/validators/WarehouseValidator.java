package com.fulfilment.application.monolith.warehouses.domain.validators;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;

@ApplicationScoped
public class WarehouseValidator {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public WarehouseValidator(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  /** Validates a brand new warehouse. Returns the resolved location for the caller's reuse. */
  public Location validateForCreate(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WebApplicationException(
          "A warehouse with business unit code "
              + warehouse.businessUnitCode
              + " already exists.",
          400);
    }

    Location location = resolveLocationOrThrow(warehouse.location);
    validateCapacityAtLeastStock(warehouse);
    validateLocationQuota(location, warehouse.location, warehouse.capacity, null);
    return location;
  }

  /**
   * Validates a replacement warehouse against the active warehouse it is replacing. Returns the
   * resolved location for the caller's reuse.
   */
  public Location validateForReplace(Warehouse newWarehouse, Warehouse existing) {
    Location location = resolveLocationOrThrow(newWarehouse.location);
    validateCapacityAtLeastStock(newWarehouse);

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

    validateLocationQuota(
        location, newWarehouse.location, newWarehouse.capacity, existing.businessUnitCode);
    return location;
  }

  private Location resolveLocationOrThrow(String identifier) {
    Location location = locationResolver.resolveByIdentifier(identifier);
    if (location == null) {
      throw new WebApplicationException("Unknown location: " + identifier, 400);
    }
    return location;
  }

  private void validateCapacityAtLeastStock(Warehouse warehouse) {
    if (warehouse.capacity == null
        || warehouse.stock == null
        || warehouse.capacity < warehouse.stock) {
      throw new WebApplicationException("Warehouse capacity must be at least its stock.", 400);
    }
  }

  /**
   * @param excludeBuCode when replacing a warehouse, its own business unit code must be excluded
   *     from the "active warehouses at this location" count/capacity so it isn't counted twice;
   *     pass null when creating a brand new warehouse.
   */
  private void validateLocationQuota(
      Location location, String locationIdentifier, int newCapacity, String excludeBuCode) {
    List<Warehouse> activeAtLocation =
        warehouseStore.getAll().stream()
            .filter(w -> w.location.equals(locationIdentifier) && w.archivedAt == null)
            .filter(w -> excludeBuCode == null || !w.businessUnitCode.equals(excludeBuCode))
            .toList();

    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WebApplicationException(
          "Location " + location.identification + " has reached its maximum number of warehouses.",
          400);
    }

    int usedCapacity = activeAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (usedCapacity + newCapacity > location.maxCapacity) {
      throw new WebApplicationException(
          "Location " + location.identification + " does not have enough remaining capacity.",
          400);
    }
  }
}
