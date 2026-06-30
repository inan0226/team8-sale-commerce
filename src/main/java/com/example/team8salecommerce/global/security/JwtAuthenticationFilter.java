package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 HTTP 요청에서 Authorization 헤더의 JWT를 확인하는 Spring Security 필터입니다.
 *
 * <p>필터는 컨트롤러보다 먼저 실행됩니다. 따라서 보호된 API에 요청이 도착하기 전에
 * 토큰을 읽고, 유효한 토큰이면 SecurityContext에 인증 정보를 저장합니다.</p>
 *
 * <p>SecurityContext에 인증 정보가 들어가면 이후 컨트롤러에서
 * {@code @AuthenticationPrincipal AuthMember authMember} 형태로 로그인 회원 정보를 받을 수 있습니다.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final BearerTokenResolver bearerTokenResolver;
    private final JwtAuthenticationService jwtAuthenticationService;

    /**
     * 요청 하나당 한 번 실행되는 필터 메서드입니다.
     *
     * <p>토큰이 없는 요청은 일단 익명 상태로 다음 필터로 넘깁니다.
     * 그 요청이 공개 API인지, 로그인이 필요한 API인지는 {@link SecurityConfig}의 인가 규칙이 판단합니다.</p>
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = bearerTokenResolver.resolveOptional(request.getHeader("Authorization"))
                .orElse(null);

        try {
            if (StringUtils.hasText(token)) {
                authenticate(token);
            }

            filterChain.doFilter(request, response);
        } catch (CustomException exception) {
            SecurityContextHolder.clearContext();
            writeErrorResponse(response, exception.getErrorCode());
        }
    }

    /**
     * 토큰을 검증하고, 검증된 회원 정보를 Spring Security 인증 객체로 바꿉니다.
     */
    private void authenticate(String token) {
        Authentication authentication = jwtAuthenticationService.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 인증/인가 과정에서 발생한 예외를 공통 API 응답 형식으로 내려줍니다.
     *
     * <p>필터 단계에서 예외가 발생하면 일반 컨트롤러 예외 처리까지 가지 못할 수 있으므로
     * 여기서 직접 JSON 응답을 작성합니다.</p>
     */
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"message":"%s","data":null}
                """.formatted(errorCode.getMessage()));
    }
}
