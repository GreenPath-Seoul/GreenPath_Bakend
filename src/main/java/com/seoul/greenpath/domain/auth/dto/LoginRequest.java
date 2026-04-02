package com.seoul.greenpath.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 일반 로그인 요청 DTO
 */
@Schema(description = "로그인 요청 데이터")
public record LoginRequest(

        @Schema(description = "이메일 주소", example = "user@greenpath.seoul.kr")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {}
