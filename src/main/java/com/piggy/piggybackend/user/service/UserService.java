package com.piggy.piggybackend.user.service;

import com.piggy.piggybackend.global.exception.CustomException;
import com.piggy.piggybackend.global.exception.ErrorCode;
import com.piggy.piggybackend.global.service.S3Service;
import com.piggy.piggybackend.global.util.CryptoUtil;
import com.piggy.piggybackend.global.util.JwtUtil;
import com.piggy.piggybackend.user.dto.*;
import com.piggy.piggybackend.user.entity.Provider;
import com.piggy.piggybackend.user.entity.User;
import com.piggy.piggybackend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CryptoUtil cryptoUtil;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;
    private final S3Service s3Service;
    private final FcmTokenService fcmTokenService;


    private static final String RESET_PREFIX = "pwd_reset:";

    @Value("${app.default-image-url}")
    private String defaultProfileImageUrl;

    public void signUp(UserCreateRequest request) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 비밀번호 확인
        if (!request.getPassword().equals(request.getCheckedPassword())) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }

        // 비밀번호 해싱
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 전화번호 AES 암호화
        String encryptedPhone = cryptoUtil.encrypt(request.getPhoneNum());

        // User 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .nickname(request.getNickname())
                .birth(request.getBirth())
                .phoneNum(encryptedPhone)
                .profileImageUrl(defaultProfileImageUrl)
                .provider(Provider.LOCAL)
                .providerId(null)
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        // 1. 탈퇴 유예 중인 회원인지 확인
        if (user.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.USER_WITHDRAWN, "탈퇴 진행 중인 계정입니다. 계정을 복구하시겠습니까?");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // ✨ Redis에 리프레시 토큰 저장 (TTL 14일) - 버그 수정
        redisTemplate.opsForValue().set("RT:" + user.getEmail(), refreshToken, 14, TimeUnit.DAYS);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    // 이메일 중복체크 (형식 검증은 DTO/@Valid에서 수행)
    @Transactional(readOnly = true)
    public EmailCheckResponse checkEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        return new EmailCheckResponse(true);
    }

    // 닉네임 중복체크 (길이/형식 검증은 DTO/@Valid에서 수행)
    @Transactional(readOnly = true)
    public EmailCheckResponse checkNickname(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
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

    @Transactional
    public void createResetToken(String email, String source, String redirectUrl) {
        userRepository.findByEmail(email).ifPresent(user -> {

            // 1. 고유 토큰 생성
            String token = UUID.randomUUID().toString();
            String key = RESET_PREFIX + token;

            // 2. Redis에 저장 (Key: 토큰, Value: 이메일)
            // 30분 뒤 자동으로 사라지도록 설정 (유효기간 30분)
            redisTemplate.opsForValue().set(key, email, 30, TimeUnit.MINUTES);

            // 3. 메일 발송
            mailService.sendResetMail(email, token, source, redirectUrl);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String key = RESET_PREFIX + token;

        // 1. Redis에서 토큰으로 이메일 조회
        String email = redisTemplate.opsForValue().get(key);

        // 2. 토큰이 없거나 만료된 경우
        if (email == null) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        // 3. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. 기존 비밀번호 확인
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD);
        }

        // 5. 비밀번호 업데이트
        user.updatePassword(passwordEncoder.encode(newPassword));

        // 6. 사용 완료된 토큰은 즉시 삭제
        redisTemplate.delete(key);
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

        String currentImageUrl = user.getProfileImageUrl();
        if (currentImageUrl != null && !currentImageUrl.equals(defaultProfileImageUrl)) {
            s3Service.delete(currentImageUrl);
        }

        // 새 프사 업로드 및 DB 업데이트
        String imageUrl = s3Service.upload(image, "profiles");
        user.updateProfileImage(imageUrl);

        return imageUrl;
    }

    //프로필사진 삭제
    @Transactional
    public String deleteProfileImage(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String currentImageUrl = user.getProfileImageUrl();
        if (currentImageUrl != null && !currentImageUrl.equals(defaultProfileImageUrl)) {
            s3Service.delete(currentImageUrl);
        }

        user.updateProfileImage(defaultProfileImageUrl);

        return defaultProfileImageUrl;
    }

    @Transactional
    public void withdraw(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이미 탈퇴한 회원인지 체크 (선택사항)
        if (user.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "이미 탈퇴 처리된 회원입니다.");
        }

        user.softDelete();
    }

    // 스케줄러용: 1달 지난 탈퇴 회원 영구 삭제
    @Transactional
    public void deleteExpiredUsers() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        userRepository.deleteByDeletedAtBefore(oneMonthAgo);
    }

    // 계정 복구 로직
    @Transactional
    public void recoverAccount(LoginRequest request) {

        // 1. 유저 조회 (탈퇴한 유저도 조회되어야 하므로 일반 findByEmail 사용)
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 본인 확인을 위해 비밀번호 한 번 더 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 탈퇴한 계정이 맞는지 확인
        if (user.getDeletedAt() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "탈퇴 상태인 계정이 아닙니다.");
        }

        // 4. 복구 (deletedAt 초기화)
        user.recover();
    }
    // 알림 관련 서비스 내 토큰 업데이트 로직
    @Transactional
    public void updateFcmToken(User user, String rawToken) {
        String encryptedToken = cryptoUtil.encrypt(rawToken);

        fcmTokenService.saveOrUpdateToken(user, encryptedToken);
    }
}
