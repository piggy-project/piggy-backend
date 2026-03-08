package com.piggy.piggybackend.user.service;

import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.service.S3Service;
import com.piggy.piggybackend.global.util.CryptoUtil;
import com.piggy.piggybackend.global.util.JwtUtil;
import com.piggy.piggybackend.user.dto.*;
import com.piggy.piggybackend.user.entity.PasswordResetToken;
import com.piggy.piggybackend.user.entity.Provider;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.PasswordResetTokenRepository;
import com.piggy.piggybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CryptoUtil cryptoUtil;
    private final JwtUtil jwtUtil;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final S3Service s3Service;
    private String defaultProfileImage;

    public void signUp(UserCreateRequest request) {

        // 1️⃣ 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2️⃣ 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 3️⃣ 비밀번호 확인
        if (!request.getPassword().equals(request.getCheckedPassword())) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }

        // 4️⃣ 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5️⃣ 전화번호 AES 암호화
        String encryptedPhone = cryptoUtil.encrypt(request.getPhoneNum());

        // 6️⃣ User 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .nickname(request.getNickname())
                .birth(request.getBirth())
                .phoneNum(encryptedPhone)
                .provider(Provider.LOCAL)
                .providerId(null)
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    //이메일 중복체크
    public EmailCheckResponse checkEmail(String email) {
        System.out.println("클라이언트로부터 받은 이메일 값: " + email);
        // null 또는 공백
        if (email == null || email.isBlank()) {
            throw new CustomException(
                    ErrorCode.VALIDATION_ERROR,
                    "올바른 이메일 형식이 아닙니다."
            );
        }

        // 형식 체크
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!email.matches(emailRegex)) {
            throw new CustomException(
                    ErrorCode.VALIDATION_ERROR,
                    "올바른 이메일 형식이 아닙니다."
            );
        }

        // 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(
                    ErrorCode.DUPLICATE_EMAIL,
                    "이미 등록된 이메일입니다."
            );
        }

        return new EmailCheckResponse(true);
    }

    //닉네임 중복체크
    public EmailCheckResponse checkNickname(String nickname) {

        // null 또는 공백
        if (nickname == null || nickname.isBlank()) {
            throw new CustomException(
                    ErrorCode.VALIDATION_ERROR,
                    "닉네임 형식이 아닙니다."
            );
        }

        // 길이 제한 (2~20자)
        if (nickname.length() < 2 || nickname.length() > 20) {
            throw new CustomException(
                    ErrorCode.VALIDATION_ERROR,
                    "닉네임 형식이 아닙니다."
            );
        }

        // 중복 체크
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(
                    ErrorCode.DUPLICATE_NICKNAME,
                    "이미 사용중인 닉네임입니다."
            );
        }

        return new EmailCheckResponse(true);
    }
    // 내정보 조회
    @Transactional(readOnly = true)
    public UserInfoResponse getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.USER_NOT_FOUND,
                        "존재하지 않는 사용자입니다."
                ));
        return new UserInfoResponse(user);
    }

    public void createResetToken(String email) {

        userRepository.findByEmail(email).ifPresent(user -> {

            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(30))
                    .build();

            passwordResetTokenRepository.save(resetToken);

            mailService.sendResetMail(email, token);
        });

        // 존재 안 해도 그냥 성공 응답
    }

    //비밀번호 변경
    @Transactional
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        User user = resetToken.getUser();


        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));

        passwordResetTokenRepository.delete(resetToken);
    }
    @Transactional
    public void updateAdditionalInfo(String email, AdditionalInfoRequest request) {
        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 정보 업데이트 (더미 데이터를 실제 데이터로 교체)
        user.updateSocialInfo(
                request.getNickname(),
                LocalDate.parse(request.getBirth()), // YYYY-MM-DD 포맷
                request.getPhoneNum()
        );

        // 3. (선택) 만약 ROLE_GUEST 권한을 썼다면 여기서 ROLE_USER로 변경
        // user.updateRole(Role.USER);

        userRepository.save(user);
    }
    @Transactional
    public void deleteUnfinishedUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 추가 정보를 아직 입력하지 않은(1900년생 더미인) 경우에만 삭제 가능하도록 설정
        if (user.getBirth().equals(java.time.LocalDate.of(1900, 1, 1))) {
            userRepository.delete(user);
        } else {
            throw new CustomException(ErrorCode.INVALID_REQUEST); // 이미 가입 완료된 유저는 삭제 불가
        }
    }

    // 닉네임 변경
    @Transactional
    public void updateNickname(String email, NicknameUpdateRequest request) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newNickname = request.getNickname();

        // 2. 현재 닉네임과 동일한지 확인
        if (user.getNickname().equals(newNickname)) {
            throw new CustomException(
                    ErrorCode.DUPLICATE_NICKNAME,
                    "현재 사용 중인 닉네임입니다."
            );
        }

        // 3. 닉네임 중복 체크
        if (userRepository.existsByNickname(newNickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 4. 닉네임 업데이트
        user.updateNickname(newNickname);
    }

    //S3 프로필사진
    @Transactional
    public String updateProfileImage(String email, MultipartFile image) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 프사가 있으면 S3에서 삭제
        if (user.getProfileImageUrl() != null) {
            s3Service.delete(user.getProfileImageUrl());
        }

        // 새 프사 업로드 및 DB 업데이트
        String imageUrl = s3Service.upload(image, "profiles");
        user.updateProfileImage(imageUrl);

        return imageUrl;
    }

    //프로필사진 삭제
    @Transactional
    public void deleteProfileImage(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. S3에 실제 파일이 있다면 삭제
        if (user.getProfileImageUrl() != null) {
            s3Service.delete(user.getProfileImageUrl());
        }

        // 2. DB 경로를 null로 업데이트
        user.updateProfileImage(null);
    }
}
