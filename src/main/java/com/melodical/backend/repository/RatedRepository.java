package com.melodical.backend.repository;

import com.melodical.backend.entity.Rated;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface RatedRepository extends JpaRepository<Rated, Long> {
    Optional<Rated> findByUserIdAndContentTypeAndContentId(Long userId, String contentType, String contentId);
    List<Rated> findAllByUserIdAndContentType(Long userId, String contentType);
    
    // 평점 기반 추천을 위한 메서드
    List<Rated> findByUserIdAndContentType(Long userId, String contentType);
}
