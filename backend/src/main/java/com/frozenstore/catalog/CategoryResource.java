package com.frozenstore.catalog;

import com.frozenstore.catalog.dto.CategoryRequest;
import com.frozenstore.catalog.dto.CategoryResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * 商品分類 REST 端點
 *
 * GET /api/categories        → 公開，任何人都可以查詢分類
 * POST/PUT/DELETE             → 需要 ADMIN 角色
 */
@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

    @Inject
    CategoryService categoryService;

    @GET
    @Consumes(MediaType.WILDCARD)
    public List<CategoryResponse> listAll() {
        return categoryService.listAll();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response create(@Valid CategoryRequest req) {
        CategoryResponse response = categoryService.create(req);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public CategoryResponse update(@PathParam("id") Long id, @Valid CategoryRequest req) {
        return categoryService.update(id, req);
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Long id) {
        categoryService.delete(id);
        return Response.noContent().build();
    }
}
