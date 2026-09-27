package com.board.member;

import com.board.global.exception.BusinessException;
import com.board.global.exception.ErrorCode;
import com.board.global.security.JwtTokenProvider;
import com.board.member.dto.LoginRequest;
import com.board.member.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        boolean matches = passwordEncoder.matches(request.password(), member.getPassword());
        if (!matches) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getEmail());

        return new TokenResponse(accessToken);
    }
}
