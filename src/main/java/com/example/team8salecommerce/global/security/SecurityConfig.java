package com.example.team8salecommerce.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security의 전체 HTTP 인증/인가 규칙을 정의하는 설정 클래스입니다.
 *
 * <p>초보자 관점에서 이 클래스는 "어떤 URL은 로그인 없이 허용하고,
 * 어떤 URL은 로그인한 사용자만 허용할지"를 정하는 곳입니다.</p>
 *
 * <p>이 프로젝트는 서버 세션을 사용하는 로그인 방식이 아니라 JWT 방식을 사용합니다.
 * 따라서 요청마다 Authorization 헤더에 담긴 토큰을 확인하고,
 * 토큰이 유효하면 Spring Security가 "로그인한 사용자"로 인식하게 만듭니다.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	/**
	 * HTTP 요청이 들어올 때 적용할 보안 필터 체인을 만듭니다.
	 *
	 * <p>처리 순서는 크게 다음과 같습니다.</p>
	 * <ol>
	 *     <li>{@link JwtAuthenticationFilter}가 Authorization 헤더에서 JWT를 찾습니다.</li>
	 *     <li>JWT가 유효하면 SecurityContext에 인증된 사용자 정보를 넣습니다.</li>
	 *     <li>아래 인가 규칙에 따라 URL 접근 가능 여부를 판단합니다.</li>
	 * </ol>
	 *
	 * <p>인증은 "누구인지 확인하는 것"이고, 인가는 "그 사람이 이 URL에 접근해도 되는지 확인하는 것"입니다.</p>
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
			// JWT 기반 무상태 인증을 사용하므로 서버 세션과 기본 로그인 방식을 끕니다.
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(jwtAccessDeniedHandler)
			)
			.authorizeHttpRequests(auth -> auth
				// 시연용 정적 페이지는 로그인 전에도 접근할 수 있어야 하므로 공개합니다.
				.requestMatchers("/demo.html", "/favicon.ico").permitAll()
				// 회원가입과 로그인은 토큰이 없는 사용자가 호출해야 하므로 공개합니다.
				.requestMatchers("/auth/signup", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
				// WebSocket 연결(handshake)은 허용하고, 실제 STOMP 인증은 WebSocketJwtChannelInterceptor에서 처리합니다.
				.requestMatchers("/ws/chat", "/ws/chat/**").permitAll()
				// 상품/카테고리 조회처럼 누구나 볼 수 있는 조회 API는 공개합니다.
				.requestMatchers(HttpMethod.GET, "/products", "/products/*", "/products/search").permitAll()
				.requestMatchers(HttpMethod.GET, "/categories/*/products").permitAll()
				.requestMatchers(HttpMethod.GET, "/search-keywords/top").permitAll()
				// 관리자용 API는 ADMIN 권한이 있는 사용자만 접근할 수 있습니다.
				.requestMatchers(HttpMethod.GET, "/search-keywords").hasRole("ADMIN")
				.requestMatchers("/admin/**").hasRole("ADMIN")
				// 위에서 따로 허용하지 않은 나머지 API는 모두 로그인한 사용자만 접근할 수 있습니다.
				.anyRequest().authenticated()
			)
			// 기본 username/password 인증 필터보다 먼저 JWT를 해석해 SecurityContext에 인증 정보를 넣습니다.
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.build();
	}

	/**
	 * 비밀번호 암호화에 사용할 인코더를 Spring Bean으로 등록합니다.
	 *
	 * <p>{@link BCryptPasswordEncoder}는 같은 비밀번호라도 매번 다른 해시 값을 만들 수 있어
	 * 단순 해시보다 안전한 방식입니다.</p>
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
