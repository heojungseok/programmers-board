package com.board.member;

import com.board.global.exception.BusinessException;
import com.board.global.exception.ErrorCode;
import com.board.member.dto.MemberResponse;
import com.board.member.dto.SignUpRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse joinMember(SignUpRequest request) {
        boolean existed = memberRepository.existsByEmail(request.email());
        if (existed) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }

        String encoded = passwordEncoder.encode(request.password());

        Member member = memberRepository.save(
                new Member(
                        request.email(),
                        encoded,
                        request.nickname()
                )
        );

        return MemberResponse.from(member);
    }
}
