package com.melodical.backend.repository

import com.melodical.backend.domain.PostLike
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PostLikeRepository : JpaRepository<PostLike, Long> {
    fun findByUserIdAndPostId(userId: Long, postId: Long): Optional<PostLike>
    fun countByPostId(postId: Long): Long
}