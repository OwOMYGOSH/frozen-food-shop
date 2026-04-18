package com.frozenstore.auth.dto;

import jakarta.validation.constraints.*;

/**
 * 註冊請求 DTO
 *
 * 為什麼用 record：
 * - 不可變（immutable），請求資料不應該被修改
 * - 自動產生 constructor、getter、equals、hashCode、toString
 * - 比傳統 class 少很多 boilerplate
 *
 * 驗證邏輯放在這裡（Bean Validation），不放在 Service 裡，
 * 讓 Service 只處理業務邏輯，不處理格式驗證。
 */
public record RegisterRequest(

    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    @Size(max = 255, message = "Email 最多 255 字元")
    String email,

    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 100, message = "密碼長度需在 8～100 字元之間")
    String password,

    @NotBlank(message = "姓名不能為空")
    @Size(max = 100, message = "姓名最多 100 字元")
    String name,

    @Size(max = 20, message = "電話最多 20 字元")
    String phone  // 選填，null 也可以

) {}
