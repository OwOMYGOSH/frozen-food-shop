package com.frozenstore.order;

import com.frozenstore.order.dto.CreateOrderRequest;
import com.frozenstore.order.dto.OrderResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("CUSTOMER")
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOrder(@Context SecurityContext sec, @Valid CreateOrderRequest req) {
        Long userId = extractUserId(sec);
        OrderResponse order = orderService.createOrder(userId, req);
        return Response.status(Response.Status.CREATED).entity(order).build();
    }

    @GET
    @Consumes(MediaType.WILDCARD)
    public List<OrderResponse> listMyOrders(@Context SecurityContext sec) {
        return orderService.listMyOrders(extractUserId(sec));
    }

    @GET
    @Path("/{id}")
    @Consumes(MediaType.WILDCARD)
    public OrderResponse getOrder(@Context SecurityContext sec, @PathParam("id") Long id) {
        return orderService.getOrder(extractUserId(sec), id);
    }

    private Long extractUserId(SecurityContext sec) {
        return Long.parseLong(sec.getUserPrincipal().getName());
    }
}
