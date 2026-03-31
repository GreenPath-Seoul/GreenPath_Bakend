package com.seoul.greenpath.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 일반 회원가입 요청 DTO
 */
@Schema(description = "회원가입 요청 데이터")
public record SignUpRequest(

        @Schema(description = "이메일 주소", example = "user@greenpath.seoul.kr")
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @Schema(description = "비밀번호 (8~20자 영문, 숫자, 특수문자 포함)", example = "Password123!")
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        @jakarta.validation.constraints.Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 8~20자의 영문, 숫자, 특수문자를 포함해야 합니다."
        )
        String password,

        @Schema(description = "사용자 이름", example = "홍길동")
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 1, max = 20, message = "이름은 1~20자여야 합니다.")
        String name
) {}
