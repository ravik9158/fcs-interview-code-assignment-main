package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validators.WarehouseValidator;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

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
      identifier -> "ZWOLLE-001".equals(identifier) ? new Location("ZWOLLE-001", 1, 40) : null;

  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    stored.clear();
    useCase = new CreateWarehouseUseCase(warehouseStore, new WarehouseValidator(warehouseStore, locationResolver));
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
  public void testHappyPathCreatesWarehouse() {
    useCase.create(newWarehouse("MWH.100", "ZWOLLE-001", 30, 10));

    assertEquals(1, stored.size());
    assertEquals("MWH.100", stored.get(0).businessUnitCode);
  }

  @Test
  public void testDuplicateBusinessUnitCodeIsRejected() {
    useCase.create(newWarehouse("MWH.100", "ZWOLLE-001", 30, 10));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.create(newWarehouse("MWH.100", "ZWOLLE-001", 10, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testUnknownLocationIsRejected() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.create(newWarehouse("MWH.101", "NOWHERE", 10, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testCapacityLowerThanStockIsRejected() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.create(newWarehouse("MWH.101", "ZWOLLE-001", 5, 10)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testMaxNumberOfWarehousesAtLocationIsEnforced() {
    // ZWOLLE-001 allows only 1 warehouse.
    useCase.create(newWarehouse("MWH.100", "ZWOLLE-001", 20, 5));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.create(newWarehouse("MWH.101", "ZWOLLE-001", 10, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testMaxCapacityAtLocationIsEnforced() {
    var locationResolverWithRoom =
        (LocationResolver) identifier -> new Location("AMSTERDAM-001", 5, 40);
    useCase =
        new CreateWarehouseUseCase(
            warehouseStore, new WarehouseValidator(warehouseStore, locationResolverWithRoom));

    useCase.create(newWarehouse("MWH.100", "AMSTERDAM-001", 30, 5));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> useCase.create(newWarehouse("MWH.101", "AMSTERDAM-001", 15, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
