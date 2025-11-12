package com.melodical.backend.config

import com.melodical.backend.config.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod 
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .csrf { it.disable() }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // 1. 로그인, 회원가입, Swagger는 누구나 접근(permitAll)
                    .requestMatchers(
                        "/api/auth/**",
                        "/api/users/signup",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                    ).permitAll()
                    // 2. 게시판 조회(GET)는 누구나 접근(permitAll)
                    .requestMatchers(
                        HttpMethod.GET, 
                        "/api/posts", 
                        "/api/posts/**"
                    ).permitAll()
                    // 3. 관리자(admin) API는 ADMIN 권한 필요
                    .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                    // 4. 그 외 모든 요청은 로그인(인증) 필요
                    .anyRequest().authenticated()
            }
            // 5. JWT 필터를 Spring Security 필터 체인에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
