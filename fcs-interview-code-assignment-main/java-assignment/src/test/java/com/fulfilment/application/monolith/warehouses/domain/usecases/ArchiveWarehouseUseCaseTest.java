package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private int updateCallCount = 0;

  private final WarehouseStore warehouseStore =
      new WarehouseStore() {
        @Override
        public List<Warehouse> getAll() {
          return List.of();
        }

        @Override
        public void create(Warehouse warehouse) {}

        @Override
        public void update(Warehouse warehouse) {
          updateCallCount++;
        }

        @Override
        public void remove(Warehouse warehouse) {}

        @Override
        public Warehouse findByBusinessUnitCode(String buCode) {
          return null;
        }
      };

  private final ArchiveWarehouseUseCase useCase = new ArchiveWarehouseUseCase(warehouseStore);

  @Test
  public void testArchivingSetsArchivedAtAndPersists() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";

    useCase.archive(warehouse);

    assertNotNull(warehouse.archivedAt);
    assertEquals(1, updateCallCount);
  }

  @Test
  public void testArchivingAlreadyArchivedWarehouseIsNoOp() {
    var warehouse = new Warehouse();
    warehouse.businessUnitCode = "MWH.100";
    warehouse.archivedAt = java.time.LocalDateTime.now().minusDays(1);
    var originalArchivedAt = warehouse.archivedAt;

    useCase.archive(warehouse);

    assertEquals(originalArchivedAt, warehouse.archivedAt);
    assertEquals(0, updateCallCount);
  }
}
