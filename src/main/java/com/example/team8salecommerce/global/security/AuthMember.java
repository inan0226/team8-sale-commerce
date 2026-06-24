package com.example.team8salecommerce.global.security;

import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.entity.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record AuthMember(
        Long memberId,
        String email,
        Role role
) {

    public static AuthMember from(Member member) {
        return new AuthMember(member.getId(), member.getEmail(), member.getRole());
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
