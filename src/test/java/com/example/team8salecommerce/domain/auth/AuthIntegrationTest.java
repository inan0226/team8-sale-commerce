package com.example.team8salecommerce.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

	/**
	 * 인증 통합 테스트는 Redis Lock 기능을 직접 검증하지 않는다.
	 *
	 * 하지만 애플리케이션 컨텍스트가 로딩될 때 RedissonClient Bean이 필요하므로,
	 * 테스트 환경에서는 실제 Redis(localhost:6379)에 연결하지 않도록 Mock으로 대체한다.
	 */
	@MockitoBean
	private RedissonClient redissonClient;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 회원가입_성공() throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "signup-success@example.com",
                                  "password": "Password123!",
                                  "nickname": "signup-success"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("signup-success@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("signup-success"));
    }

    @Test
    void 이메일이_중복되면_회원가입에_실패한다() throws Exception {
        String request = """
                {
                  "email": "duplicate@example.com",
                  "password": "Password123!",
                  "nickname": "duplicate-first"
                }
                """;

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "duplicate@example.com",
                                  "password": "Password123!",
                                  "nickname": "duplicate-second"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 로그인_성공_후_내_정보를_조회한다() throws Exception {
        signup("login-success@example.com", "Password123!", "login-success");

        MvcResult loginResult = login("login-success@example.com", "Password123!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Secure")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Strict")))
                .andReturn();

        String accessToken = readAccessToken(loginResult);

        mockMvc.perform(get("/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("login-success@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("login-success"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void 비밀번호가_틀리면_로그인에_실패한다() throws Exception {
        signup("invalid-password@example.com", "Password123!", "invalid-password");

        login("invalid-password@example.com", "WrongPassword123!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 토큰이_없으면_보호된_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 잘못된_토큰이면_보호된_API에_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/members/me")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
    }

    @Test
    void 로그아웃_후_같은_토큰으로_보호된_API에_접근할_수_없다() throws Exception {
        signup("logout@example.com", "Password123!", "logout");

        MvcResult loginResult = login("logout@example.com", "Password123!")
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = readAccessToken(loginResult);

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Max-Age=0")));

        mockMvc.perform(get("/members/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 리프레시_토큰_쿠키로_액세스_토큰을_재발급한다() throws Exception {
        signup("refresh@example.com", "Password123!", "refresh");

        MvcResult loginResult = login("refresh@example.com", "Password123!")
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = readRefreshToken(loginResult);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=")));
    }

    @Test
    void 재발급에_사용한_이전_리프레시_토큰은_다시_사용할_수_없다() throws Exception {
        signup("refresh-rotate@example.com", "Password123!", "refresh-rotate");

        MvcResult loginResult = login("refresh-rotate@example.com", "Password123!")
                .andExpect(status().isOk())
                .andReturn();

        String oldRefreshToken = readRefreshToken(loginResult);

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(new Cookie("refreshToken", oldRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void signup(String email, String password, String nickname) throws Exception {
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s",
                                  "nickname": "%s"
                                }
                                """.formatted(email, password, nickname)))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password)));
    }

    private String readAccessToken(MvcResult loginResult) throws Exception {
        String response = loginResult.getResponse().getContentAsString();
        String marker = "\"accessToken\":\"";
        int startIndex = response.indexOf(marker) + marker.length();
        int endIndex = response.indexOf("\"", startIndex);
        return response.substring(startIndex, endIndex);
    }

    private String readRefreshToken(MvcResult loginResult) {
        String setCookie = loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        Assertions.assertThat(setCookie).isNotBlank();

        String marker = "refreshToken=";
        int startIndex = setCookie.indexOf(marker) + marker.length();
        int endIndex = setCookie.indexOf(";", startIndex);
        return setCookie.substring(startIndex, endIndex);
    }
}
