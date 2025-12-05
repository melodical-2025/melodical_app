package com.melodical.backend.service;

import com.melodical.backend.dto.UserLoginResponse;
import com.melodical.backend.dto.UserProfileResponse;
import com.melodical.backend.dto.UserStatsResponse;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.UserRepository;
import com.melodical.backend.repository.FavoriteRepository;
import com.melodical.backend.repository.RatedMusicalRepository;
import com.melodical.backend.repository.RatedRepository;
import com.melodical.backend.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 인증 및 프로필 관리 서비스
 * 주의: 인증 관련 메서드(register, login)는 레거시 호환을 위해 유지하지만,
 * 새로운 인증은 AuthService를 사용하세요.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final FavoriteRepository favoriteRepository;
    private final RatedMusicalRepository ratedMusicalRepository;
    private final RatedRepository ratedRepository;

    /**
     * 회원가입 (레거시 호환용 - AuthService 사용 권장)
     */
    @Deprecated
    public User register(String email, String password) {
        String encodedPassword = passwordEncoder.encode(password);
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .build();
        return userRepository.save(user);
    }

    /**
     * 로그인 (레거시 호환용 - AuthService 사용 권장)
     */
    @Deprecated
    public UserLoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId());
        return new UserLoginResponse(user.getEmail(), token);
    }

    // ===== 프로필 관리 메서드 =====

    /**
     * 이메일로 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    /**
     * ID로 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    /**
     * 닉네임 변경
     */
    @Transactional
    public UserProfileResponse updateNickname(String email, String newNickname) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (newNickname == null || newNickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
        
        user.setName(newNickname);  // User 엔티티의 name 필드가 nickname으로 사용됨
        userRepository.save(user);
        
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    /**
     * 프로필 이미지 업데이트
     */
    @Transactional
    public UserProfileResponse updateProfileImage(String email, String profileImageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 기존 이미지 삭제 (있을 경우)
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            fileStorageService.deleteFile(user.getProfileImageUrl());
        }
        
        user.setProfileImageUrl(profileImageUrl);
        userRepository.save(user);
        
        return UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 소셜 로그인 사용자는 비밀번호 변경 불가
        if (user.getProvider() != null && !user.getProvider().equals("local")) {
            throw new IllegalArgumentException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
        }
        
        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호 유효성 검사
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("새 비밀번호는 최소 6자 이상이어야 합니다.");
        }
        
        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 추가 로직: 사용자 관련 데이터 삭제 (리뷰, 위시리스트 등)
        // TODO: 연관된 데이터 cascade 삭제 구현
        
        userRepository.delete(user);
    }

    /**
     * 사용자 통계 정보 조회 (찜, 평가 개수 포함)
     */
    public UserStatsResponse getUserStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 찜한 뮤지컬 개수
        int favoriteCount = (int) favoriteRepository.countByUserId(userId);
        
        // 평가한 뮤지컬 개수
        int ratedMusicalCount = ratedMusicalRepository.findByUserId(userId).size();
        
        // 평가한 음악 개수 (contentType이 "music"인 것들)
        int ratedMusicCount = (int) ratedRepository.findByUserIdAndContentType(userId, "music").size();
        
        return UserStatsResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .favoriteCount(favoriteCount)
                .ratedMusicalCount(ratedMusicalCount)
                .ratedMusicCount(ratedMusicCount)
                .build();
    }
}
