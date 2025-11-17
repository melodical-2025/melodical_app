package com.melodical.backend.repository;

import com.melodical.backend.entity.InteractionLog;
import com.melodical.backend.entity.Musical;
import com.melodical.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InteractionLogRepository extends JpaRepository<InteractionLog, Long> {

    List<InteractionLog> findByUserAndInteractionType(User user, String interactionType);

    @Query("SELECT il FROM InteractionLog il WHERE il.user = :user " +
           "AND il.interactionType = 'rating' AND il.ratingValue >= :minRating")
    List<InteractionLog> findHighRatingsByUser(@Param("user") User user,
                                              @Param("minRating") Double minRating);

    @Query("SELECT il.musical, AVG(il.ratingValue) FROM InteractionLog il " +
           "WHERE il.interactionType = 'rating' AND il.ratingValue IS NOT NULL " +
           "GROUP BY il.musical HAVING COUNT(il) >= :minRatings")
    List<Object[]> findAverageRatingsByMusical(@Param("minRatings") Long minRatings);

    @Query("SELECT il FROM InteractionLog il WHERE il.exposureLog.id = :exposureLogId")
    List<InteractionLog> findByExposureLogId(@Param("exposureLogId") Long exposureLogId);

    @Query("SELECT COUNT(il) FROM InteractionLog il WHERE il.exposureLog.id IN :exposureLogIds " +
           "AND il.interactionType = 'click'")
    Long countClicksByExposureLogIds(@Param("exposureLogIds") List<Long> exposureLogIds);

    @Query("SELECT il.musical, COUNT(il) FROM InteractionLog il " +
           "WHERE il.interactionType = :interactionType " +
           "AND il.interactionAt >= :startTime " +
           "GROUP BY il.musical ORDER BY COUNT(il) DESC")
    List<Object[]> findTopInteractionsByType(@Param("interactionType") String interactionType,
                                           @Param("startTime") LocalDateTime startTime);

    @Query("SELECT DISTINCT il.user FROM InteractionLog il " +
           "WHERE il.musical = :musical AND il.interactionType = 'rating' " +
           "AND il.ratingValue >= :minRating")
    List<User> findUsersByMusicalRating(@Param("musical") Musical musical,
                                       @Param("minRating") Double minRating);

    List<InteractionLog> findByUserAndInteractionAtBetween(User user, LocalDateTime startTime, LocalDateTime endTime);

}
