package com.frozenstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 進入點
 *
 * 對應 Quarkus：Quarkus 沒有顯式的 main class，走 quarkus-maven-plugin 自動產生。
 * Spring Boot 明確要有一個 @SpringBootApplication 類別當啟動點。
 *
 * @SpringBootApplication 等同於三個註解的組合：
 *   @Configuration       — 宣告這個類別有 @Bean 定義
 *   @EnableAutoConfiguration — 根據 classpath 自動設定（Web、JPA、Security...）
 *   @ComponentScan       — 掃描本 package 以下的 @Component / @Service / @Repository / @RestController
 */
@SpringBootApplication
public class FrozenFoodShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrozenFoodShopApplication.class, args);
    }
}
