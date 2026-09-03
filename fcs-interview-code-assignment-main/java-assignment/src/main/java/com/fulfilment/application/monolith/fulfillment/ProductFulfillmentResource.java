package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfillment")
@Produces("application/json")
@Consumes("application/json")
public class ProductFulfillmentResource {

  private static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  private static final int MAX_WAREHOUSES_PER_STORE = 3;
  private static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @Inject ProductFulfillmentRepository fulfillmentRepository;
  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  public record FulfillmentRequest(Long storeId, Long productId, Long warehouseId) {}

  @POST
  @Transactional
  public Response associate(FulfillmentRequest request) {
    Store store = Store.findById(request.storeId());
    if (store == null) {
      throw new WebApplicationException("Store not found", 404);
    }
    Product product = productRepository.findById(request.productId());
    if (product == null) {
      throw new WebApplicationException("Product not found", 404);
    }
    DbWarehouse warehouse = warehouseRepository.findById(request.warehouseId());
    if (warehouse == null) {
      throw new WebApplicationException("Warehouse not found", 404);
    }

    if (fulfillmentRepository.existsAssociation(
        request.storeId(), request.productId(), request.warehouseId())) {
      throw new WebApplicationException(
          "This product is already fulfilled by this warehouse for this store.", 400);
    }

    if (fulfillmentRepository.countDistinctWarehousesForProductInStore(
            request.storeId(), request.productId())
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException(
          "This product is already fulfilled by "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses in this store.",
          400);
    }

    if (fulfillmentRepository.countDistinctWarehousesForStore(request.storeId())
        >= MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException(
          "This store is already fulfilled by " + MAX_WAREHOUSES_PER_STORE + " warehouses.", 400);
    }

    if (fulfillmentRepository.countDistinctProductsForWarehouse(request.warehouseId())
        >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new WebApplicationException(
          "This warehouse already stocks " + MAX_PRODUCTS_PER_WAREHOUSE + " product types.", 400);
    }

    fulfillmentRepository.persist(new ProductFulfillment(store, product, warehouse));
    return Response.status(201).build();
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfillmentRequest> listForStore(@PathParam("storeId") Long storeId) {
    return fulfillmentRepository
        .find("store.id = ?1", storeId)
        .list()
        .stream()
        .map(pf -> new FulfillmentRequest(pf.store.id, pf.product.id, pf.warehouse.id))
        .toList();
  }
}
