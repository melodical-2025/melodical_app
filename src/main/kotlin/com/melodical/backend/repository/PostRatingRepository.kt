package com.melodical.backend.repository

import com.melodical.backend.domain.PostRating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface PostRatingRepository : JpaRepository<PostRating, Long> {
    fun findByUserIdAndPostId(userId: Long, postId: Long): Optional<PostRating>

    @Query("SELECT AVG(pr.rating) FROM PostRating pr WHERE pr.post.id = :postId")
    fun findAverageRatingByPostId(postId: Long): Double?
}