package com.example.team8salecommerce.domain.auth.dto;

import com.example.team8salecommerce.domain.member.entity.Member;

public record SignupResponse(
        Long memberId,
        String email,
        String nickname
) {

    public static SignupResponse from(Member member) {
        return new SignupResponse(member.getId(), member.getEmail(), member.getNickname());
    }
}
