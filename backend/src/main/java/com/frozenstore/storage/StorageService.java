package com.frozenstore.storage;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayInputStream;
import java.util.UUID;

/**
 * MinIO 物件儲存服務
 *
 * 職責：上傳圖片檔案，回傳可存取的 URL
 *
 * 設計重點：
 * - @PostConstruct 失敗不中斷啟動（MinIO 可能比後端慢啟動）
 * - upload() 在 MinIO 不可用時拋出 RuntimeException，由 ErrorMapper 處理
 * - 每個上傳的物件名稱加 UUID 前綴，避免重複覆蓋
 */
@ApplicationScoped
public class StorageService {

    @ConfigProperty(name = "minio.url", defaultValue = "http://localhost:9000")
    String url;

    @ConfigProperty(name = "minio.access-key", defaultValue = "minioadmin")
    String accessKey;

    @ConfigProperty(name = "minio.secret-key", defaultValue = "minioadmin123")
    String secretKey;

    @ConfigProperty(name = "minio.bucket", defaultValue = "frozen-food-images")
    String bucket;

    private MinioClient client;

    @PostConstruct
    void init() {
        try {
            client = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();
            ensureBucketExists();
        } catch (Exception e) {
            // 啟動時 MinIO 可能還沒準備好，記錄警告但不中斷啟動
            // 實際上傳時若 client == null 會拋出清楚的錯誤
            System.err.println("[StorageService] MinIO 初始化失敗（可稍後重試）: " + e.getMessage());
            client = null;
        }
    }

    /**
     * 上傳圖片，回傳可公開存取的完整 URL
     *
     * @param data        檔案位元組
     * @param filename    原始檔名（用於保留副檔名）
     * @param contentType MIME type，如 image/jpeg
     * @return 圖片公開 URL，如 http://localhost:9000/frozen-food-images/uuid-photo.jpg
     */
    public String upload(byte[] data, String filename, String contentType) {
        if (client == null) {
            throw new RuntimeException("圖片儲存服務不可用，請稍後再試");
        }

        // UUID 前綴避免檔名衝突
        String objectName = UUID.randomUUID() + "-" + sanitize(filename);

        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("圖片上傳失敗: " + e.getMessage(), e);
        }

        return url + "/" + bucket + "/" + objectName;
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = client.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    /** 移除路徑分隔符，避免 MinIO 誤判為子目錄 */
    private String sanitize(String filename) {
        if (filename == null) return "image";
        return filename.replaceAll("[/\\\\]", "_");
    }
}
