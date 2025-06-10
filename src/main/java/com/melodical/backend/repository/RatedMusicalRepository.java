package com.melodical.backend.repository;

import com.melodical.backend.entity.Musical;
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
    Optional<RatedMusical> findByUserAndMusical(User user, Musical musical);

    List<RatedMusical> findByUserId(Long userId);
}
