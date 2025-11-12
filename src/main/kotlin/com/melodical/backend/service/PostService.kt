package com.melodical.backend.service

import com.melodical.backend.domain.Post
import com.melodical.backend.dto.*
import com.melodical.backend.repository.*
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val commentRepository: CommentRepository,
    private val postLikeRepository: PostLikeRepository,
    private val postRatingRepository: PostRatingRepository
) {

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<PostSummaryResponse> {
        return postRepository.findAll(pageable).map { it.toSummaryResponse() }
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): PostResponse {
        val post = postRepository.findById(id).orElseThrow { EntityNotFoundException("Post not found with id: $id") }
        return post.toResponse()
    }

    @Transactional
    fun save(request: PostRequest): PostResponse {
        val username = SecurityContextHolder.getContext().authentication.name
        val author = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("User not found with username: $username") }
        val post = Post(title = request.title, content = request.content, author = author)
        return postRepository.save(post).toResponse()
    }

    @Transactional
    fun update(id: Long, request: PostRequest): PostResponse {
        val post = postRepository.findById(id).orElseThrow { EntityNotFoundException("Post not found with id: $id") }
        // In a real app, you should check if the user has permission to update the post
        post.title = request.title
        post.content = request.content
        return postRepository.save(post).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        // In a real app, you should check if the user has permission to delete the post
        if (!postRepository.existsById(id)) {
            throw EntityNotFoundException("Post not found with id: $id")
        }
        postRepository.deleteById(id)
    }
    
    private fun Post.toResponse(): PostResponse {
        val comments = commentRepository.findByPostId(this.id!!).map { it.toResponse() }
        val likeCount = postLikeRepository.countByPostId(this.id!!)
        val averageRating = postRatingRepository.findAverageRatingByPostId(this.id!!) ?: 0.0
        return PostResponse(
            id = this.id!!,
            title = this.title,
            content = this.content,
            authorUsername = this.author.username,
            comments = comments,
            likeCount = likeCount,
            averageRating = averageRating
        )
    }

    private fun Post.toSummaryResponse(): PostSummaryResponse = PostSummaryResponse(
        id = this.id!!,
        title = this.title,
        authorUsername = this.author.username
    )

    private fun com.melodical.backend.domain.Comment.toResponse(): CommentResponse = CommentResponse(
        id = this.id!!,
        content = this.content,
        authorUsername = this.author.username
    )
}