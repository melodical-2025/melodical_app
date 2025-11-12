package com.melodical.backend.controller

import com.melodical.backend.dto.PostRequest
import com.melodical.backend.dto.PostResponse
import com.melodical.backend.dto.PostSummaryResponse
import com.melodical.backend.service.PostService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/posts")
class PostController(private val postService: PostService) {

    @GetMapping
    fun getAllPosts(pageable: Pageable): ResponseEntity<Page<PostSummaryResponse>> {
        return ResponseEntity.ok(postService.findAll(pageable))
    }

    @GetMapping("/{id}")
    fun getPostById(@PathVariable id: Long): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.findById(id))
    }

    @PostMapping
    fun createPost(@RequestBody request: PostRequest): ResponseEntity<PostResponse> {
        val post = postService.save(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(post)
    }

    @PutMapping("/{id}")
    fun updatePost(@PathVariable id: Long, @RequestBody request: PostRequest): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.update(id, request))
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<Void> {
        postService.delete(id)
        return ResponseEntity.noContent().build()
    }
}