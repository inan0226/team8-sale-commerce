package com.example.team8salecommerce.domain.auth.dto;

import com.example.team8salecommerce.domain.member.entity.Member;

/**
 * 회원가입 성공 시 클라이언트에게 내려주는 응답입니다.
 *
 * <p>비밀번호는 응답에 절대 포함하지 않습니다. 회원가입 결과 확인에 필요한 최소 정보만 반환합니다.</p>
 *
 * @param memberId 가입된 회원 ID
 * @param email 가입된 회원 이메일
 * @param nickname 가입된 회원 닉네임
 */
public record SignupResponse(
        Long memberId,
        String email,
        String nickname
) {

    /**
     * Member 엔티티에서 응답에 필요한 값만 골라 SignupResponse로 변환합니다.
     */
    public static SignupResponse from(Member member) {
        return new SignupResponse(member.getId(), member.getEmail(), member.getNickname());
    }
}
