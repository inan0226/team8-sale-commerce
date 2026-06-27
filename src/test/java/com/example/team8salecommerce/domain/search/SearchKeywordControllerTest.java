package com.example.team8salecommerce.domain.search;

import com.example.team8salecommerce.domain.search.dto.SearchKeywordResponse;
import com.example.team8salecommerce.domain.search.service.SearchKeywordService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
@AutoConfigureMockMvc(addFilters = false)
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
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].keyword").value("에어팟"))
                .andExpect(jsonPath("$[0].count").value(523))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].keyword").value("맥북"))
                .andExpect(jsonPath("$[1].count").value(312));
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
}
