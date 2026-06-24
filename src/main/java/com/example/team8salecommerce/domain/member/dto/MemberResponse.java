package com.example.team8salecommerce.domain.member.dto;

import com.example.team8salecommerce.domain.member.entity.Member;

public record MemberResponse(
        Long memberId,
        String email,
        String nickname,
        String role
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name()
        );
    }
}
