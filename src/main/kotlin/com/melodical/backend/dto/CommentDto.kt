package com.melodical.backend.dto

data class CommentRequest(
    val content: String
)

data class CommentResponse(
    val id: Long,
    val content: String,
    val authorUsername: String
)