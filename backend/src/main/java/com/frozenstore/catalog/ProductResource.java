package com.frozenstore.catalog;

import com.frozenstore.catalog.dto.*;
import com.frozenstore.storage.StorageService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import java.nio.file.Files;
import java.util.Map;

/**
 * 商品 REST 端點
 *
 * GET  /api/products                 → 公開，分頁列表（可篩選分類/關鍵字）
 * GET  /api/products/{id}            → 公開，單一商品詳情
 * POST/PUT/DELETE                    → 需要 ADMIN 角色
 * POST /api/products/{id}/images     → 需要 ADMIN 角色，上傳商品圖片到 MinIO
 */
@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject
    ProductService productService;

    @Inject
    StorageService storageService;

    /**
     * GET /api/products
     *
     * Query params：
     *   category  - 分類 slug（如 seafood）
     *   keyword   - 商品名稱關鍵字
     *   page      - 頁碼，從 0 開始（預設 0）
     *   size      - 每頁筆數，1～100（預設 20）
     *   sort      - price_asc / price_desc / name_asc / created_desc（預設）
     */
    @GET
    @Consumes(MediaType.WILDCARD)
    public ProductPageResponse list(
        @QueryParam("category") String category,
        @QueryParam("keyword")  String keyword,
        @QueryParam("page")     @DefaultValue("0")  int page,
        @QueryParam("size")     @DefaultValue("20") int size,
        @QueryParam("sort")     @DefaultValue("created_desc") String sort
    ) {
        if (size < 1 || size > 100) size = 20;
        if (page < 0) page = 0;
        return productService.listPage(category, keyword, page, size, sort);
    }

    @GET
    @Path("/{id}")
    @Consumes(MediaType.WILDCARD)
    public ProductResponse getById(@PathParam("id") Long id) {
        return productService.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response create(@Valid ProductRequest req) {
        ProductResponse response = productService.create(req);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public ProductResponse update(@PathParam("id") Long id, @Valid ProductRequest req) {
        return productService.update(id, req);
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") Long id) {
        productService.softDelete(id);
        return Response.noContent().build();
    }

    /**
     * POST /api/products/{id}/images
     *
     * 上傳商品圖片到 MinIO，回傳圖片 URL
     *
     * Form params：
     *   file       - 圖片檔案（必填）
     *   isPrimary  - 是否設為主圖（預設 false）
     */
    @POST
    @Path("/{id}/images")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("ADMIN")
    @Transactional
    public Response uploadImage(
        @PathParam("id") Long id,
        @RestForm("file") FileUpload file,
        @RestForm("isPrimary") @DefaultValue("false") boolean isPrimary
    ) throws Exception {
        ProductEntity product = ProductEntity.findById(id);
        if (product == null) throw new NotFoundException("商品不存在：" + id);

        byte[] data = Files.readAllBytes(file.uploadedFile());
        String contentType = file.contentType() != null ? file.contentType() : "image/jpeg";
        String url = storageService.upload(data, file.fileName(), contentType);

        if (isPrimary) {
            // 先把舊主圖取消
            ProductImageEntity.update("isPrimary = false WHERE productId = ?1", id);
        }

        ProductImageEntity image = new ProductImageEntity();
        image.productId  = id;
        image.url        = url;
        image.isPrimary  = isPrimary;
        image.sortOrder  = (int) ProductImageEntity.count("productId", id);
        image.persist();

        return Response.status(Response.Status.CREATED)
            .entity(Map.of("url", url, "isPrimary", isPrimary))
            .build();
    }
}
