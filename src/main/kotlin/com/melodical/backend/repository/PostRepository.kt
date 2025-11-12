package com.melodical.backend.repository

import com.melodical.backend.domain.Post
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PostRepository : JpaRepository<Post, Long> {
	fun findByTitle(title: String): Optional<Post>
}
