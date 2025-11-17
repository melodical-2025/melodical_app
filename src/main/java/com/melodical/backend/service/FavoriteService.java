package com.melodical.backend.service;

import com.melodical.backend.entity.Favorite;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.FavoriteRepository;
import com.melodical.backend.repository.MusicalRepository;
import com.melodical.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final MusicalRepository musicalRepository;

    /**
     * 찜 추가
     */
    @Transactional
    public Favorite addFavorite(String email, Long musicalId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        Musical musical = musicalRepository.findById(musicalId)
                .orElseThrow(() -> new IllegalArgumentException("뮤지컬을 찾을 수 없습니다: " + musicalId));

        // 이미 찜했는지 확인
        if (favoriteRepository.existsByUserIdAndMusicalId(user.getId(), musicalId)) {
            throw new IllegalArgumentException("이미 찜한 뮤지컬입니다.");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .musical(musical)
                .build();

        return favoriteRepository.save(favorite);
    }

    /**
     * 찜 취소
     */
    @Transactional
    public void removeFavorite(String email, Long musicalId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        Favorite favorite = favoriteRepository.findByUserIdAndMusicalId(user.getId(), musicalId)
                .orElseThrow(() -> new IllegalArgumentException("찜한 내역이 없습니다."));

        favoriteRepository.delete(favorite);
    }

    /**
     * 찜 토글 (있으면 삭제, 없으면 추가)
     */
    @Transactional
    public boolean toggleFavorite(String email, Long musicalId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        Musical musical = musicalRepository.findById(musicalId)
                .orElseThrow(() -> new IllegalArgumentException("뮤지컬을 찾을 수 없습니다: " + musicalId));

        if (favoriteRepository.existsByUserIdAndMusicalId(user.getId(), musicalId)) {
            favoriteRepository.deleteByUserIdAndMusicalId(user.getId(), musicalId);
            return false; // 찜 취소
        } else {
            Favorite favorite = Favorite.builder()
                    .user(user)
                    .musical(musical)
                    .build();
            favoriteRepository.save(favorite);
            return true; // 찜 추가
        }
    }

    /**
     * 사용자의 찜 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesByUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * 찜 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isFavorite(String email, Long musicalId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        return favoriteRepository.existsByUserIdAndMusicalId(user.getId(), musicalId);
    }

    /**
     * 사용자의 찜 개수
     */
    @Transactional(readOnly = true)
    public long getFavoriteCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        return favoriteRepository.countByUserId(user.getId());
    }
}
