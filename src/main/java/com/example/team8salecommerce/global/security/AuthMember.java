package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.entity.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Spring Security가 들고 다닐 "현재 로그인한 회원" 정보입니다.
 *
 * <p>DB의 {@link Member} 엔티티 전체를 SecurityContext에 넣지 않고,
 * 인증/인가에 필요한 회원 ID, 이메일, 역할만 가볍게 담습니다.</p>
 *
 * @param memberId 로그인한 회원의 ID
 * @param email 로그인한 회원의 이메일
 * @param role 로그인한 회원의 역할(USER, ADMIN 등)
 */
public record AuthMember(
        Long memberId,
        String email,
        Role role
) {

    /**
     * Member 엔티티에서 인증에 필요한 값만 뽑아 AuthMember로 변환합니다.
     */
    public static AuthMember from(Member member) {
        return new AuthMember(member.getId(), member.getEmail(), member.getRole());
    }

    /**
     * Spring Security가 이해하는 권한 목록으로 변환합니다.
     *
     * <p>Spring Security의 {@code hasRole("ADMIN")} 규칙은 내부적으로
     * {@code ROLE_ADMIN} 권한을 찾습니다. 그래서 역할 이름 앞에 {@code ROLE_}을 붙입니다.</p>
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
