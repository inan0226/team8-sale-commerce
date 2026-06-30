package com.example.team8salecommerce.domain.auth.service;

import com.example.team8salecommerce.domain.auth.dto.LoginRequest;
import com.example.team8salecommerce.domain.auth.dto.LoginResponse;
import com.example.team8salecommerce.domain.auth.dto.SignupRequest;
import com.example.team8salecommerce.domain.auth.dto.SignupResponse;
import com.example.team8salecommerce.domain.auth.dto.TokenRefreshResponse;
import com.example.team8salecommerce.domain.member.entity.Member;
import com.example.team8salecommerce.domain.member.repository.MemberRepository;
import com.example.team8salecommerce.global.exception.CustomException;
import com.example.team8salecommerce.global.exception.ErrorCode;
import com.example.team8salecommerce.global.security.AuthMember;
import com.example.team8salecommerce.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입, 로그인, 로그아웃의 실제 인증 로직을 담당하는 서비스입니다.
 *
 * <p>컨트롤러가 요청을 받아 이 서비스로 넘기면, 서비스는 다음 일을 처리합니다.</p>
 * <ul>
 *     <li>회원가입: 이메일/닉네임 중복 확인, 비밀번호 암호화, 회원 저장</li>
 *     <li>로그인: 회원 조회, 비밀번호 검증, Access Token/Refresh Token 발급</li>
 *     <li>로그아웃: Access Token 검증, blacklist 등록, Refresh Token 삭제</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * 회원가입을 처리합니다.
     *
     * <p>비밀번호는 절대 원문 그대로 저장하지 않고 {@link PasswordEncoder}로 암호화합니다.
     * 이렇게 저장해야 DB가 노출되어도 실제 비밀번호를 바로 알 수 없습니다.</p>
     */
    public SignupResponse signup(SignupRequest request) {
        validateDuplicateMember(request);

        Member member = Member.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname()
        );

        return SignupResponse.from(memberRepository.save(member));
    }

    /**
     * 로그인을 처리하고 JWT 두 종류를 발급합니다.
     *
     * <p>Access Token은 API 인증에 사용하고, Refresh Token은 Access Token을 다시 발급받기 위한 용도입니다.
     * 이 프로젝트에서는 Refresh Token을 Redis에 저장해 서버가 유효한 Refresh Token인지 확인할 수 있게 합니다.</p>
     */
    public LoginResult login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        validatePassword(request.password(), member.getPassword());

        AuthMember authMember = AuthMember.from(member);
        String accessToken = jwtTokenProvider.createAccessToken(authMember);
        String refreshToken = jwtTokenProvider.createRefreshToken(authMember);
        refreshTokenService.saveLoginSession(
                authMember,
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );

        LoginResponse response = new LoginResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                accessToken,
                "Bearer"
        );

        return new LoginResult(response, refreshToken, jwtTokenProvider.getRefreshTokenExpirationMillis());
    }

    public TokenRefreshResult refreshAccessToken(String refreshToken) {
        JwtTokenProvider.TokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);
        AuthMember authMember = refreshTokenService.validateRefreshToken(claims.memberId(), refreshToken);

        String accessToken = jwtTokenProvider.createAccessToken(authMember);
        String rotatedRefreshToken = jwtTokenProvider.createRefreshToken(authMember);
        refreshTokenService.saveLoginSession(
                authMember,
                rotatedRefreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );

        return new TokenRefreshResult(
                new TokenRefreshResponse(accessToken, "Bearer"),
                rotatedRefreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );
    }

    /**
     * 로그아웃을 처리합니다.
     *
     * <p>이미 발급된 JWT는 만료 시간 전까지 문자열 자체로는 유효합니다.
     * 그래서 Access Token의 남은 만료 시간만큼 blacklist에 등록해 재사용을 막고,
     * 회원의 Refresh Token도 삭제합니다.</p>
     */
    public void logout(String accessToken) {
        JwtTokenProvider.TokenClaims claims = jwtTokenProvider.parseAccessToken(accessToken);
        long remainingMillis = claims.expiresAtMillis() - System.currentTimeMillis();

        refreshTokenService.blacklistAccessToken(accessToken, remainingMillis);
        refreshTokenService.deleteRefreshToken(claims.memberId());
    }

    private void validateDuplicateMember(SignupRequest request) {
        memberRepository.findByEmailOrNickname(request.email(), request.nickname())
                .ifPresent(member -> {
                    if (member.getEmail().equals(request.email())) {
                        throw new CustomException(ErrorCode.DUPLICATED_EMAIL);
                    }

                    throw new CustomException(ErrorCode.DUPLICATED_NICKNAME);
                });
    }

    /**
     * 사용자가 입력한 비밀번호와 DB에 암호화되어 저장된 비밀번호가 같은지 확인합니다.
     *
     * <p>암호화된 비밀번호는 직접 문자열 비교를 하면 안 되고,
     * 반드시 {@link PasswordEncoder#matches(CharSequence, String)}로 비교해야 합니다.</p>
     */
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
