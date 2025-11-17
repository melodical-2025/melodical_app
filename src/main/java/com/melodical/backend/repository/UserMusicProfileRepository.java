package com.melodical.backend.repository;

import com.melodical.backend.entity.User;
import com.melodical.backend.entity.UserMusicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMusicProfileRepository extends JpaRepository<UserMusicProfile, Long> {

    List<UserMusicProfile> findByUser(User user);

    Optional<UserMusicProfile> findByUserAndGenreName(User user, String genreName);

    @Query("SELECT ump FROM UserMusicProfile ump WHERE ump.user = :user ORDER BY ump.preferenceScore DESC")
    List<UserMusicProfile> findByUserOrderByPreferenceScoreDesc(@Param("user") User user);

    @Query("SELECT ump.genreName, AVG(ump.preferenceScore) FROM UserMusicProfile ump " +
           "WHERE ump.user IN :users GROUP BY ump.genreName")
    List<Object[]> findAveragePreferenceByGenreForUsers(@Param("users") List<User> users);

    @Query("SELECT u FROM User u JOIN UserMusicProfile ump ON u = ump.user " +
           "WHERE ump.genreName = :genreName AND ump.preferenceScore >= :minScore")
    List<User> findUsersByGenrePreference(@Param("genreName") String genreName,
                                         @Param("minScore") Double minScore);
}
