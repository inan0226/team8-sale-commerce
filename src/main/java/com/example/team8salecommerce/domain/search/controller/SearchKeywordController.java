package com.example.team8salecommerce.domain.search.controller;

import com.example.team8salecommerce.domain.search.dto.SearchKeywordResponse;
import com.example.team8salecommerce.domain.search.service.SearchKeywordService;
import com.example.team8salecommerce.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchKeywordController {

    private final SearchKeywordService searchKeywordService;

    @GetMapping("/search-keywords/top")
    public ResponseEntity<ApiResponse<List<SearchKeywordResponse>>> getTopKeywords() {
        List<SearchKeywordResponse> response = searchKeywordService.getTopKeywords();
        return ResponseEntity.ok(ApiResponse.success("인기 검색어 조회 성공", response));
    }
}
