package com.melodical.backend.service

import com.melodical.backend.domain.PostLike
import com.melodical.backend.domain.PostRating
import com.melodical.backend.dto.PostRatingRequest
import com.melodical.backend.repository.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostInteractionService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val postLikeRepository: PostLikeRepository,
    private val postRatingRepository: PostRatingRepository
) {

    @Transactional
    fun likePost(postId: Long) {
        val user = getCurrentUser()
        val post = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }

        if (postLikeRepository.findByUserIdAndPostId(user.id!!, postId).isPresent) {
            throw IllegalStateException("User has already liked this post")
        }

        postLikeRepository.save(PostLike(user = user, post = post))
    }

    @Transactional
    fun unlikePost(postId: Long) {
        val user = getCurrentUser()
        val like = postLikeRepository.findByUserIdAndPostId(user.id!!, postId)
            .orElseThrow { IllegalStateException("User has not liked this post") }
        postLikeRepository.delete(like)
    }

    @Transactional
    fun ratePost(postId: Long, request: PostRatingRequest) {
        val user = getCurrentUser()
        val post = postRepository.findById(postId).orElseThrow { EntityNotFoundException("Post not found") }

        val existingRating = postRatingRepository.findByUserIdAndPostId(user.id!!, postId)
        if (existingRating.isPresent) {
            val rating = existingRating.get()
            rating.rating = request.rating
            postRatingRepository.save(rating)
        } else {
            postRatingRepository.save(PostRating(user = user, post = post, rating = request.rating))
        }
    }

    private fun getCurrentUser() = userRepository.findByUsername(SecurityContextHolder.getContext().authentication.name)
        .orElseThrow { EntityNotFoundException("User not found") }
}