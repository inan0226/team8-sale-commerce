package com.example.team8salecommerce.domain.search.service;

import com.example.team8salecommerce.domain.search.dto.SearchKeywordResponse;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchKeywordService {

    private static final String KEYWORD_RANKING_KEY = "popular:search";
    private final StringRedisTemplate stringRedisTemplate;

    public void incrementKeywordCount(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        try {
            stringRedisTemplate.opsForZSet().incrementScore(KEYWORD_RANKING_KEY, keyword.trim(), 1.0);
        } catch (Exception exception) {
            log.warn("검색어 스코어 증가 실패: {}", keyword, exception);
        }
    }

    public List<SearchKeywordResponse> getTopKeywords() {
        try {
            Set<ZSetOperations.TypedTuple<String>> typedTuples =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(KEYWORD_RANKING_KEY, 0, 9);

            if (typedTuples == null) {
                return List.of();
            }

            List<SearchKeywordResponse> response = new ArrayList<>();
            int rank = 1;
            for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
                String keyword = tuple.getValue();
                Double score = tuple.getScore();
                long count = score != null ? score.longValue() : 0L;
                response.add(new SearchKeywordResponse(rank++, keyword, count));
            }
            return response;
        } catch (Exception exception) {
            log.error("인기 검색어 조회 중 오류 발생", exception);
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_READ_FAILED);
        }
    }
}
