package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private final List<Warehouse> stored = new ArrayList<>();
  private final WarehouseStore warehouseStore =
      new WarehouseStore() {
        @Override
        public List<Warehouse> getAll() {
          return stored;
        }

        @Override
        public void create(Warehouse warehouse) {
          stored.add(warehouse);
        }

        @Override
        public void update(Warehouse warehouse) {}

        @Override
        public void remove(Warehouse warehouse) {}

        @Override
        public Warehouse findByBusinessUnitCode(String buCode) {
          return stored.stream()
              .filter(w -> w.businessUnitCode.equals(buCode) && w.archivedAt == null)
              .findFirst()
              .orElse(null);
        }
      };

  private final LocationResolver locationResolver =
      identifier -> "ZWOLLE-002".equals(identifier) ? new Location("ZWOLLE-002", 2, 50) : null;

  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    stored.clear();
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  private Warehouse existingWarehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    stored.add(warehouse);
    return warehouse;
  }

  private Warehouse newWarehouse(String buCode, String location, int capacity, int stock) {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testHappyPathArchivesOldAndCreatesNew() {
    Warehouse existing = existingWarehouse("MWH.100", "ZWOLLE-002", 30, 10);

    useCase.replace(newWarehouse("MWH.100", "ZWOLLE-002", 40, 10));

    assertNotNull(existing.archivedAt);
    assertEquals(2, stored.size());
    Warehouse created = stored.get(1);
    assertEquals("MWH.100", created.businessUnitCode);
    assertEquals(40, created.capacity);
    assertNull(created.archivedAt);
  }

  @Test
  public void testMissingExistingWarehouseIsRejected() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.replace(newWarehouse("MWH.999", "ZWOLLE-002", 40, 10)));
    assertEquals(404, ex.getResponse().getStatus());
  }

  @Test
  public void testNewCapacityBelowOldStockIsRejected() {
    existingWarehouse("MWH.100", "ZWOLLE-002", 30, 20);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.replace(newWarehouse("MWH.100", "ZWOLLE-002", 15, 15)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testStockMismatchIsRejected() {
    existingWarehouse("MWH.100", "ZWOLLE-002", 30, 10);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.replace(newWarehouse("MWH.100", "ZWOLLE-002", 40, 15)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testMaxCapacityAtLocationIsEnforcedExcludingReplacedWarehouse() {
    existingWarehouse("MWH.100", "ZWOLLE-002", 30, 10);
    existingWarehouse("MWH.101", "ZWOLLE-002", 15, 5);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.replace(newWarehouse("MWH.100", "ZWOLLE-002", 40, 10)));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
