package com.example.team8salecommerce.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청에서 클라이언트가 보내는 값입니다.
 *
 * <p>{@code @Email}, {@code @NotBlank}, {@code @Size}는 컨트롤러에 도착한 입력값을
 * 서비스 로직으로 넘기기 전에 먼저 검증합니다. 검증에 실패하면 회원가입 로직은 실행되지 않습니다.</p>
 *
 * @param email 로그인 ID로 사용할 이메일
 * @param password 로그인에 사용할 비밀번호
 * @param nickname 서비스에서 표시할 닉네임
 */
public record SignupRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname
) {
}
