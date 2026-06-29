package com.example.team8salecommerce.domain.search.service;

import com.example.team8salecommerce.domain.search.dto.SearchKeywordResponse;
import com.example.team8salecommerce.domain.search.dto.SearchKeywordStatsResponse;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchKeywordService {

    private static final String KEYWORD_RANKING_KEY = "popular:search";
    private static final String KEYWORD_LAST_SEARCHED_KEY = "search:last-searched";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final StringRedisTemplate stringRedisTemplate;

    public void incrementKeywordCount(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        try {
            String trimmedKeyword = keyword.trim();
            stringRedisTemplate.opsForZSet().incrementScore(KEYWORD_RANKING_KEY, trimmedKeyword, 1.0);
            
            String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            stringRedisTemplate.opsForHash().put(KEYWORD_LAST_SEARCHED_KEY, trimmedKeyword, now);
        } catch (Exception exception) {
            log.warn("검색어 스코어 및 시간 갱신 실패: {}", keyword, exception);
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

    public List<SearchKeywordStatsResponse> getSearchKeywordStats() {
        try {
            Set<ZSetOperations.TypedTuple<String>> typedTuples =
                    stringRedisTemplate.opsForZSet().reverseRangeWithScores(KEYWORD_RANKING_KEY, 0, -1);

            if (typedTuples == null || typedTuples.isEmpty()) {
                return List.of();
            }

            List<String> keywords = typedTuples.stream()
                    .map(ZSetOperations.TypedTuple::getValue)
                    .filter(Objects::nonNull)
                    .toList();

            List<Object> lastSearchedTimes = stringRedisTemplate.opsForHash()
                    .multiGet(KEYWORD_LAST_SEARCHED_KEY, new ArrayList<>(keywords));

            List<SearchKeywordStatsResponse> response = new ArrayList<>();
            int idx = 0;
            for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
                String keyword = tuple.getValue();
                Double score = tuple.getScore();
                long count = score != null ? score.longValue() : 0L;

                Object lastSearchedObj = (lastSearchedTimes != null && idx < lastSearchedTimes.size())
                        ? lastSearchedTimes.get(idx++)
                        : null;
                String lastSearchedAt = lastSearchedObj != null ? lastSearchedObj.toString() : "";

                response.add(new SearchKeywordStatsResponse(keyword, count, lastSearchedAt));
            }
            return response;
        } catch (Exception exception) {
            log.error("검색어 통계 조회 중 오류 발생", exception);
            throw new CustomException(ErrorCode.SEARCH_KEYWORD_READ_FAILED);
        }
    }
}
