package com.melodical.backend.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class PostRatingRequest(
    @field:Min(1, message = "Rating must be at least 1")
    @field:Max(5, message = "Rating must be at most 5")
    val rating: Int
)