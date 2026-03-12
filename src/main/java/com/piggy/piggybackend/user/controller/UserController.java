package com.piggy.piggybackend.user.controller;

import com.piggy.piggybackend.global.response.ApiResponse;
import com.piggy.piggybackend.user.dto.*;
import com.piggy.piggybackend.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> signUp(
            @Valid @RequestBody UserCreateRequest request) {

        userService.signUp(request);

        return ApiResponse.success("가입이 완료되었습니다.");
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = userService.login(request);

        return ResponseEntity.status(201).body(response);
    }

    // 이메일 중복체크
    @PostMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(
            @RequestParam String email
    ) {
        EmailCheckResponse response = userService.checkEmail(email);
        return ResponseEntity.ok(response);
    }

    // 닉네임 중복체크
    @PostMapping("/check-nickname")
    public ResponseEntity<EmailCheckResponse> checkNickname(
            @RequestParam String nickname
    ) {
        EmailCheckResponse response = userService.checkNickname(nickname);
        return ResponseEntity.ok(response);
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getMyInfo(
            @AuthenticationPrincipal String email
    ) {
        UserInfoResponse userInfo = userService.getMyInfo(email);
        return ResponseEntity.ok(userInfo);
    }

    // 비밀번호 재설정 요청
    @PostMapping("/password/reset-request")
    public ResponseEntity<?> requestReset(@RequestParam String email, @RequestParam(defaultValue = "web") String source) {
        userService.createResetToken(email, source);
        return ResponseEntity.ok("이메일 발송 완료");
    }

    // 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(
            @RequestBody PasswordResetDto dto
    ) {
        userService.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok("비밀번호 변경 완료");
    }

    // 소셜 로그인 추가 정보 업데이트
    @PatchMapping("/additional-info")
    public ApiResponse<String> updateAdditionalInfo(
            @AuthenticationPrincipal String email,
            @RequestBody AdditionalInfoRequest request) {

        userService.updateAdditionalInfo(email, request);

        return ApiResponse.success("추가 정보 입력이 완료되었습니다.");
    }

    // 소셜 가입 취소
    @DeleteMapping("/social-cancel")
    public ApiResponse<String> cancelSocialSignup(@AuthenticationPrincipal String email) {
        userService.deleteUnfinishedUser(email);
        return ApiResponse.success("소셜 가입이 취소되고 데이터가 삭제되었습니다.");
    }

    // 닉네임 변경
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/nickname")
    public ResponseEntity<ApiResponse<String>> updateNickname(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody NicknameUpdateRequest request) {

        userService.updateNickname(email, request);

        return ResponseEntity.ok(ApiResponse.success("닉네임이 변경되었습니다."));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> updateProfileImage(
            @AuthenticationPrincipal String email,
            @RequestPart("image") MultipartFile image) {

        String imageUrl = userService.updateProfileImage(email, image);

        return ResponseEntity.ok(ApiResponse.success(imageUrl));
    }

    @DeleteMapping("/profile-image")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> deleteProfileImage(
            @AuthenticationPrincipal String email) {

        String defaultImageUrl = userService.deleteProfileImage(email);

        return ResponseEntity.ok(ApiResponse.success("프로필 이미지가 삭제되었습니다.", defaultImageUrl));
    }
}