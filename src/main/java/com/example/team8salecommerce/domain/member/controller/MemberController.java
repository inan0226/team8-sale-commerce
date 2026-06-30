package com.example.team8salecommerce.domain.member.controller;

import com.example.team8salecommerce.domain.member.dto.MemberResponse;
import com.example.team8salecommerce.domain.member.service.MemberService;
import com.example.team8salecommerce.global.response.ApiResponse;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.AuthMemberResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final AuthMemberResolver authMemberResolver;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long memberId = authMemberResolver.requireMemberId(authMember);

        return ResponseEntity.ok(ApiResponse.success(memberService.getMember(memberId)));
    }
}
