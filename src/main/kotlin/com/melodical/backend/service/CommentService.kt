package com.melodical.backend.service

import com.melodical.backend.domain.Comment
import com.melodical.backend.dto.CommentRequest
import com.melodical.backend.dto.CommentResponse
import com.melodical.backend.repository.CommentRepository
import com.melodical.backend.repository.PostRepository
import com.melodical.backend.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun save(postId: Long, request: CommentRequest): CommentResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found with id: $postId") }
        val username = SecurityContextHolder.getContext().authentication.name
        val author = userRepository.findByUsername(username)
            .orElseThrow { EntityNotFoundException("User not found with username: $username") }
        val comment = Comment(content = request.content, post = post, author = author)
        return commentRepository.save(comment).toResponse()
    }

    @Transactional
    fun update(id: Long, request: CommentRequest): CommentResponse {
        val comment = commentRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Comment not found with id: $id") }
        // Add permission check here
        comment.content = request.content
        return commentRepository.save(comment).toResponse()
    }

    @Transactional
    fun delete(id: Long) {
        // Add permission check here
        if (!commentRepository.existsById(id)) {
            throw EntityNotFoundException("Comment not found with id: $id")
        }
        commentRepository.deleteById(id)
    }

    private fun Comment.toResponse(): CommentResponse = CommentResponse(
        id = this.id!!,
        content = this.content,
        authorUsername = this.author.username
    )
}