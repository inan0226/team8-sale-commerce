package com.example.team8salecommerce.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청에서 클라이언트가 보내는 값입니다.
 *
 * <p>로그인은 이메일로 회원을 찾고, 입력한 비밀번호가 DB에 저장된 암호화 비밀번호와 맞는지 확인합니다.</p>
 *
 * @param email 가입할 때 사용한 이메일
 * @param password 가입할 때 입력한 비밀번호
 */
public record LoginRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
