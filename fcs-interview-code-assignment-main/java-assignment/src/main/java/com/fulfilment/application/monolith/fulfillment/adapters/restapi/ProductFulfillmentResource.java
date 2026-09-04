package com.fulfilment.application.monolith.fulfillment.adapters.restapi;

import com.fulfilment.application.monolith.fulfillment.domain.models.FulfillmentAssociation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.AssociateFulfillmentOperation;
import com.fulfilment.application.monolith.fulfillment.domain.ports.FulfillmentStore;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
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

  @Inject AssociateFulfillmentOperation associateFulfillmentOperation;
  @Inject FulfillmentStore fulfillmentStore;
  @Inject ProductRepository productRepository;
  @Inject WarehouseRepository warehouseRepository;

  public record FulfillmentRequest(Long storeId, Long productId, Long warehouseId) {}

  @POST
  @Transactional
  public Response associate(FulfillmentRequest request) {
    if (Store.findById(request.storeId()) == null) {
      throw new WebApplicationException("Store not found", 404);
    }
    if (productRepository.findById(request.productId()) == null) {
      throw new WebApplicationException("Product not found", 404);
    }
    if (warehouseRepository.findById(request.warehouseId()) == null) {
      throw new WebApplicationException("Warehouse not found", 404);
    }

    associateFulfillmentOperation.associate(
        new FulfillmentAssociation(request.storeId(), request.productId(), request.warehouseId()));

    return Response.status(201).build();
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfillmentRequest> listForStore(@PathParam("storeId") Long storeId) {
    return fulfillmentStore.findByStore(storeId).stream()
        .map(a -> new FulfillmentRequest(a.storeId, a.productId, a.warehouseId))
        .toList();
  }
}
