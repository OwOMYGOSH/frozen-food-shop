package com.frozenstore.auth.dto;

import jakarta.validation.constraints.*;

/** 登入請求 DTO */
public record LoginRequest(

    @NotBlank(message = "Email 不能為空")
    @Email(message = "Email 格式不正確")
    String email,

    @NotBlank(message = "密碼不能為空")
    String password

) {}
