package com.melodical.backend.repository;

import com.melodical.backend.entity.RatedMusical;
import com.melodical.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatedMusicalRepository
        extends JpaRepository<RatedMusical, Long> {

    // 사용자별로 뽑아오기
    List<RatedMusical> findByUser(User user);
    
    // 사용자와 뮤지컬 ID로 찾기
    Optional<RatedMusical> findByUserAndMusicalId(User user, String musicalId);
    
    // 사용자 ID와 뮤지컬 ID로 찾기
    Optional<RatedMusical> findByUserIdAndMusicalId(Long userId, String musicalId);

    // 사용자 ID로 모든 평가 찾기
    List<RatedMusical> findByUserId(Long userId);
}
