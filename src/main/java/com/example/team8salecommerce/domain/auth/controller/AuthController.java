package com.example.team8salecommerce.domain.auth.controller;

import com.example.team8salecommerce.domain.auth.dto.LoginRequest;
import com.example.team8salecommerce.domain.auth.dto.LoginResponse;
import com.example.team8salecommerce.domain.auth.dto.SignupRequest;
import com.example.team8salecommerce.domain.auth.dto.SignupResponse;
import com.example.team8salecommerce.domain.auth.service.AuthService;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.BearerTokenResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원가입, 로그인, 로그아웃 요청을 받는 인증 컨트롤러입니다.
 *
 * <p>컨트롤러는 HTTP 요청과 응답만 담당하고, 실제 회원 생성이나 비밀번호 검증,
 * JWT 발급 같은 중요한 인증 로직은 {@link AuthService}에 위임합니다.</p>
 *
 * <p>Spring Security 설정에서 {@code /auth/signup}, {@code /auth/login}은
 * 로그인 전에도 접근할 수 있도록 허용되어 있습니다. 반대로 {@code /auth/logout}은
 * Authorization 헤더에 담긴 Access Token을 사용해 로그아웃 처리합니다.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final BearerTokenResolver bearerTokenResolver;

    /**
     * 새 회원을 가입시키는 API입니다.
     *
     * <p>회원가입은 아직 로그인 전 상태에서 호출되므로 JWT 인증이 필요하지 않습니다.
     * 요청 본문은 {@link SignupRequest}로 받고, {@code @Valid}가 이메일, 비밀번호,
     * 닉네임 같은 입력값을 먼저 검증합니다.</p>
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", authService.signup(request)));
    }

    /**
     * 이메일과 비밀번호로 로그인하고 JWT를 발급받는 API입니다.
     *
     * <p>로그인 성공 시 Access Token과 Refresh Token이 내려갑니다.
     * 이후 보호된 API를 호출할 때는 Access Token을
     * {@code Authorization: Bearer 토큰값} 형태로 보내야 합니다.</p>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", authService.login(request)));
    }

    /**
     * 현재 사용 중인 Access Token을 더 이상 사용할 수 없게 만드는 로그아웃 API입니다.
     *
     * <p>JWT는 서버가 세션을 들고 있지 않는 무상태 방식이라, 토큰 자체를 즉시 삭제할 수는 없습니다.
     * 그래서 로그아웃한 Access Token을 Redis blacklist에 저장해 이후 요청에서 거부합니다.</p>
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        authService.logout(bearerTokenResolver.resolve(authorizationHeader));
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 완료되었습니다."));
    }
}
