package com.example.team8salecommerce.domain.search;

import com.example.team8salecommerce.domain.search.dto.SearchKeywordResponse;
import com.example.team8salecommerce.domain.search.service.SearchKeywordService;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchKeywordServiceTest {

    @InjectMocks
    private SearchKeywordService searchKeywordService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Test
    @DisplayName("검색어 스코어 증가 성공")
    void incrementKeywordCount_success() {
        // given
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.incrementScore(anyString(), anyString(), anyDouble())).thenReturn(1.0);

        // when
        searchKeywordService.incrementKeywordCount("에어팟");

        // then
        verify(zSetOperations).incrementScore("popular:search", "에어팟", 1.0);
    }

    @Test
    @DisplayName("검색어 스코어 증가 실패 시 예외가 발생하지 않고 warn 로그만 남김")
    void incrementKeywordCount_fail_swallowsException() {
        // given
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.incrementScore(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("Redis connection fail"));

        // when & then (예외가 던져지지 않고 처리되어야 함)
        searchKeywordService.incrementKeywordCount("에어팟");

        verify(zSetOperations).incrementScore("popular:search", "에어팟", 1.0);
    }

    @Test
    @DisplayName("인기 검색어 조회 성공 - 정렬 및 Rank 매핑 검증")
    void getTopKeywords_success() {
        // given
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        Set<ZSetOperations.TypedTuple<String>> mockTuples = new LinkedHashSet<>();
        mockTuples.add(new ZSetOperations.TypedTuple<String>() {
            @Override
            public String getValue() { return "에어팟"; }
            @Override
            public Double getScore() { return 523.0; }
            @Override
            public int compareTo(ZSetOperations.TypedTuple<String> o) { return 0; }
        });
        mockTuples.add(new ZSetOperations.TypedTuple<String>() {
            @Override
            public String getValue() { return "맥북"; }
            @Override
            public Double getScore() { return 312.0; }
            @Override
            public int compareTo(ZSetOperations.TypedTuple<String> o) { return 0; }
        });

        when(zSetOperations.reverseRangeWithScores("popular:search", 0, 9)).thenReturn(mockTuples);

        // when
        List<SearchKeywordResponse> result = searchKeywordService.getTopKeywords();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(0).keyword()).isEqualTo("에어팟");
        assertThat(result.get(0).count()).isEqualTo(523L);
        assertThat(result.get(1).rank()).isEqualTo(2);
        assertThat(result.get(1).keyword()).isEqualTo("맥북");
        assertThat(result.get(1).count()).isEqualTo(312L);
    }

    @Test
    @DisplayName("인기 검색어 조회 시 Redis 에러 발생하면 CustomException(SEARCH_KEYWORD_READ_FAILED) 발생")
    void getTopKeywords_redisError_throwsException() {
        // given
        when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(zSetOperations.reverseRangeWithScores("popular:search", 0, 9))
                .thenThrow(new RuntimeException("Redis connection error"));

        // when & then
        assertThatThrownBy(() -> searchKeywordService.getTopKeywords())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SEARCH_KEYWORD_READ_FAILED);
    }
}
