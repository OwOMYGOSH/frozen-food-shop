package com.frozenstore;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 整合測試共用基底
 *
 * 對應 Quarkus 的 Dev Services：
 *   Quarkus: 偵測 classpath 有 postgresql driver + 沒設 datasource URL → 自動起容器
 *   Spring : 要自己用 Testcontainers 起，跑完自動收
 *
 * 繼承這個類別的測試會：
 *   1. 自動啟動 PostgreSQL + Redis 容器
 *   2. 把容器 URL 注入 Spring 的 Environment（覆蓋 application-test.yml 的預設值）
 *   3. 執行 Flyway migration（自動發生，因為 Flyway 的 enabled=true）
 *   4. 測試跑完容器自動銷毀
 *
 * 用法：
 *   @SpringBootTest
 *   class MyIntegrationTest extends IntegrationTestBase {
 *       @Test void ...
 *   }
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    // static 是因為：容器啟動慢（3-5 秒），讓所有繼承的測試類共用一份，比每個測試類各起一份快得多。
    // Testcontainers 偵測 static field 會在 JVM 關閉時統一收掉。
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("frozenshop_test")
            .withUsername("test")
            .withPassword("test");

    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    /**
     * 把容器的連線資訊塞進 Spring Environment。
     * @DynamicPropertySource 是測試專用，會在 Spring context 啟動前執行。
     */
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
