package com.example.team8salecommerce.domain.auth.service;

import com.example.team8salecommerce.domain.auth.dto.LoginRequest;
import com.example.team8salecommerce.domain.auth.dto.LoginResponse;
import com.example.team8salecommerce.domain.auth.dto.SignupRequest;
import com.example.team8salecommerce.domain.auth.dto.SignupResponse;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public SignupResponse signup(SignupRequest request) {
        validateDuplicateEmail(request.email());
        validateDuplicateNickname(request.nickname());

        Member member = Member.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );

        return SignupResponse.from(memberRepository.save(member));
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        validatePassword(request.password(), member.getPassword());

        String accessToken = jwtTokenProvider.createAccessToken(member);
        String refreshToken = jwtTokenProvider.createRefreshToken(member);
        refreshTokenService.saveRefreshToken(
                member.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );

        return new LoginResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                accessToken,
                refreshToken,
                "Bearer"
        );
    }

    public void logout(String accessToken) {
        jwtTokenProvider.validateToken(accessToken);
        Long memberId = jwtTokenProvider.getMemberId(accessToken);
        long remainingMillis = jwtTokenProvider.getRemainingExpirationMillis(accessToken);

        refreshTokenService.blacklistAccessToken(accessToken, remainingMillis);
        refreshTokenService.deleteRefreshToken(memberId);
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATED_EMAIL);
        }
    }

    private void validateDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATED_NICKNAME);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
