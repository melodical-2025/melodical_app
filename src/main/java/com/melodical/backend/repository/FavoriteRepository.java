package com.melodical.backend.repository;

import com.melodical.backend.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    
    // 사용자의 찜 목록 조회 (최신순)
    @Query("SELECT f FROM Favorite f JOIN FETCH f.musical WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 특정 사용자가 특정 뮤지컬을 찜했는지 확인
    boolean existsByUserIdAndMusicalId(Long userId, Long musicalId);
    
    // 특정 사용자의 특정 뮤지컬 찜 조회
    Optional<Favorite> findByUserIdAndMusicalId(Long userId, Long musicalId);
    
    // 사용자의 찜 개수
    long countByUserId(Long userId);
    
    // 뮤지컬의 찜 개수
    long countByMusicalId(Long musicalId);
    
    // 사용자와 뮤지컬로 삭제
    void deleteByUserIdAndMusicalId(Long userId, Long musicalId);
}
