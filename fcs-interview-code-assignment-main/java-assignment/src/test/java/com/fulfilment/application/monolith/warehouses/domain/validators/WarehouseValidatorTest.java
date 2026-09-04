package com.fulfilment.application.monolith.warehouses.domain.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class WarehouseValidatorTest {

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
      identifier ->
          switch (identifier) {
            case "ZWOLLE-001" -> new Location("ZWOLLE-001", 1, 40);
            case "ZWOLLE-002" -> new Location("ZWOLLE-002", 2, 50);
            default -> null;
          };

  private WarehouseValidator validator;

  @BeforeEach
  public void setUp() {
    stored.clear();
    validator = new WarehouseValidator(warehouseStore, locationResolver);
  }

  private Warehouse warehouse(String buCode, String location, int capacity, int stock) {
    var w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }

  // --- validateForCreate ---

  @Test
  public void testValidateForCreateHappyPathReturnsLocation() {
    Location location = validator.validateForCreate(warehouse("MWH.100", "ZWOLLE-001", 30, 10));
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  public void testValidateForCreateDuplicateBusinessUnitCodeIsRejected() {
    stored.add(warehouse("MWH.100", "ZWOLLE-001", 30, 10));

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validateForCreate(warehouse("MWH.100", "ZWOLLE-001", 10, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateForCreateUnknownLocationIsRejected() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validateForCreate(warehouse("MWH.101", "NOWHERE", 10, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateForCreateCapacityLowerThanStockIsRejected() {
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validateForCreate(warehouse("MWH.101", "ZWOLLE-001", 5, 10)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateForCreateMaxNumberOfWarehousesAtLocationIsEnforced() {
    stored.add(warehouse("MWH.100", "ZWOLLE-001", 20, 5)); // ZWOLLE-001 allows only 1

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validateForCreate(warehouse("MWH.101", "ZWOLLE-001", 10, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateForCreateMaxCapacityAtLocationIsEnforced() {
    stored.add(warehouse("MWH.100", "ZWOLLE-002", 40, 5)); // ZWOLLE-002 max capacity is 50

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> validator.validateForCreate(warehouse("MWH.101", "ZWOLLE-002", 15, 5)));
    assertEquals(400, ex.getResponse().getStatus());
  }

  // --- validateForReplace ---

  @Test
  public void testValidateForReplaceHappyPath() {
    Warehouse existing = warehouse("MWH.100", "ZWOLLE-002", 30, 10);
    stored.add(existing);

    Location location =
        validator.validateForReplace(warehouse("MWH.100", "ZWOLLE-002", 40, 10), existing);
    assertEquals("ZWOLLE-002", location.identification);
  }

  @Test
  public void testValidateForReplaceCapacityBelowOldStockIsRejected() {
    Warehouse existing = warehouse("MWH.100", "ZWOLLE-002", 30, 20);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                validator.validateForReplace(
                    warehouse("MWH.100", "ZWOLLE-002", 15, 15), existing));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateForReplaceStockMismatchIsRejected() {
    Warehouse existing = warehouse("MWH.100", "ZWOLLE-002", 30, 10);

    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                validator.validateForReplace(
                    warehouse("MWH.100", "ZWOLLE-002", 40, 15), existing));
    assertEquals(400, ex.getResponse().getStatus());
  }

  @Test
  public void testValidateForReplaceExcludesReplacedWarehouseFromQuota() {
    Warehouse existing = warehouse("MWH.100", "ZWOLLE-002", 30, 10);
    stored.add(existing);
    stored.add(warehouse("MWH.101", "ZWOLLE-002", 15, 5));

    // Excluding MWH.100 itself, only MWH.101 (cap 15) is active; 15 + 40 = 55 > maxCapacity 50.
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () ->
                validator.validateForReplace(
                    warehouse("MWH.100", "ZWOLLE-002", 40, 10), existing));
    assertEquals(400, ex.getResponse().getStatus());
  }
}
