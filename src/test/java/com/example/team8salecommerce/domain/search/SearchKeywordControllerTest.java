package com.example.team8salecommerce.domain.search;

import com.example.team8salecommerce.domain.search.dto.SearchKeywordResponse;
import com.example.team8salecommerce.domain.search.dto.SearchKeywordStatsResponse;
import com.example.team8salecommerce.domain.search.service.SearchKeywordService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SearchKeywordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchKeywordService searchKeywordService;

    @MockitoBean
    private RedissonClient redissonClient;

    @Test
    @DisplayName("인기 검색어 TOP10 조회 성공")
    void getTopKeywords_success() throws Exception {
        // given
        List<SearchKeywordResponse> mockResponse = List.of(
                new SearchKeywordResponse(1, "에어팟", 523L),
                new SearchKeywordResponse(2, "맥북", 312L)
        );
        when(searchKeywordService.getTopKeywords()).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/search-keywords/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("인기 검색어 조회 성공"))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].keyword").value("에어팟"))
                .andExpect(jsonPath("$.data[0].count").value(523))
                .andExpect(jsonPath("$.data[1].rank").value(2))
                .andExpect(jsonPath("$.data[1].keyword").value("맥북"))
                .andExpect(jsonPath("$.data[1].count").value(312));
    }

    @Test
    @DisplayName("인기 검색어 조회 중 에러 발생 시 500 반환")
    void getTopKeywords_failure() throws Exception {
        // given
        when(searchKeywordService.getTopKeywords())
                .thenThrow(new CustomException(ErrorCode.SEARCH_KEYWORD_READ_FAILED));

        // when & then
        mockMvc.perform(get("/search-keywords/top"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("인기 검색어 조회에 실패했습니다."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자 권한으로 검색어 통계 조회 성공")
    void getSearchKeywordStats_success() throws Exception {
        // given
        List<SearchKeywordStatsResponse> mockResponse = List.of(
                new SearchKeywordStatsResponse("에어팟", 523L, "2026-06-22T11:00:00")
        );
        when(searchKeywordService.getSearchKeywordStats()).thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/search-keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("검색어 통계 조회 성공"))
                .andExpect(jsonPath("$.data[0].keyword").value("에어팟"))
                .andExpect(jsonPath("$.data[0].count").value(523))
                .andExpect(jsonPath("$.data[0].lastSearchedAt").value("2026-06-22T11:00:00"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자 권한으로 검색어 통계 조회 시 403 Forbidden 반환")
    void getSearchKeywordStats_forbidden() throws Exception {
        mockMvc.perform(get("/search-keywords"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("비로그인 상태로 검색어 통계 조회 시 401 Unauthorized 반환")
    void getSearchKeywordStats_unauthorized() throws Exception {
        mockMvc.perform(get("/search-keywords"))
                .andExpect(status().isUnauthorized());
    }
}
