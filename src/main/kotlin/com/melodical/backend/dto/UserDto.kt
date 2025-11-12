package com.melodical.backend.dto

import com.melodical.backend.domain.UserRole

data class UserDto(
    val username: String,
    val email: String,
    val password: String
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val roles: Set<UserRole>
)