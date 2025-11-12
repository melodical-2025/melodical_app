package com.melodical.backend.dto

data class PostRequest(
    val title: String,
    val content: String
)

data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val authorUsername: String,
    val comments: List<CommentResponse>,
    val likeCount: Long,
    val averageRating: Double
)

data class PostSummaryResponse(
    val id: Long,
    val title: String,
    val authorUsername: String
)
