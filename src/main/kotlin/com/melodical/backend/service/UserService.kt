package com.melodical.backend.service

import com.melodical.backend.domain.User
import com.melodical.backend.dto.UserDto
import com.melodical.backend.dto.UserResponse
import com.melodical.backend.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun save(userDto: UserDto): User {
        val user = User(
            username = userDto.username,
            email = userDto.email,
            password = passwordEncoder.encode(userDto.password),
            roles = mutableSetOf(com.melodical.backend.domain.UserRole.USER)
        )
        return userRepository.save(user)
    }

    fun findAll(): List<UserResponse> {
        return userRepository.findAll().map { it.toResponse() }
    }

    private fun User.toResponse(): UserResponse = UserResponse(
        id = this.id!!,
        username = this.username,
        email = this.email,
        roles = this.roles
    )
}
