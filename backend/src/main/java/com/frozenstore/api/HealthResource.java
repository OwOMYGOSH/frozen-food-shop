package com.frozenstore.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * Phase 0 健康檢查端點
 * docker compose up 後訪問 http://localhost:8080/api/health 確認後端正常
 */
@Path("/api")
public class HealthResource {

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "service", "frozen-food-shop-backend",
            "version", "1.0.0-SNAPSHOT"
        )).build();
    }
}
