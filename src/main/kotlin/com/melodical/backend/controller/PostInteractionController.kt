package com.melodical.backend.controller

import com.melodical.backend.dto.PostRatingRequest
import com.melodical.backend.service.PostInteractionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts/{postId}")
class PostInteractionController(
    private val postInteractionService: PostInteractionService
) {

    @PostMapping("/like")
    fun likePost(@PathVariable postId: Long): ResponseEntity<Void> {
        postInteractionService.likePost(postId)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/like")
    fun unlikePost(@PathVariable postId: Long): ResponseEntity<Void> {
        postInteractionService.unlikePost(postId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/ratings")
    fun ratePost(@PathVariable postId: Long, @Valid @RequestBody request: PostRatingRequest): ResponseEntity<Void> {
        postInteractionService.ratePost(postId, request)
        return ResponseEntity.ok().build()
    }
}