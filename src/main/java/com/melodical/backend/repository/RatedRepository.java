package com.melodical.backend.repository;

import com.melodical.backend.entity.Rated;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface RatedRepository extends JpaRepository<Rated, Long> {
    Optional<Rated> findByUserIdAndContentTypeAndContentId(Long userId, String contentType, String contentId);
    List<Rated> findAllByUserIdAndContentType(Long userId, String contentType);
}