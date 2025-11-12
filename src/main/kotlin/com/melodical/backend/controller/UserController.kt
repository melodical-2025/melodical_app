package com.melodical.backend.controller

import com.melodical.backend.dto.UserDto
import com.melodical.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping("/signup")
    fun registerUser(@RequestBody userDto: UserDto): ResponseEntity<String> {
        userService.save(userDto)
        return ResponseEntity.ok("User registered successfully")
    }
}
