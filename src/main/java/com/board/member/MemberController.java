package com.board.member;

import com.board.global.response.ApiResponse;
import com.board.member.dto.MemberResponse;
import com.board.member.dto.SignUpRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> joinMember(@Valid @RequestBody SignUpRequest request) {
        MemberResponse response = memberService.joinMember(request);

        return ApiResponse.success("SUCCESS", "회원 가입 성공", response);
    }
}
