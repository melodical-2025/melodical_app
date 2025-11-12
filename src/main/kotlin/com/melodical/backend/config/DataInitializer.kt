package com.melodical.backend.config

import com.melodical.backend.domain.Post 
import com.melodical.backend.domain.User
import com.melodical.backend.domain.UserRole
import com.melodical.backend.repository.PerformanceRepository 
import com.melodical.backend.repository.PostRepository 
import com.melodical.backend.repository.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val performanceRepository: PerformanceRepository, 
    private val postRepository: PostRepository 
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // 1. 'admin' 유저가 없으면 생성 (기존 코드)
        val adminUser = userRepository.findByUsername("admin").orElseGet {
            userRepository.save(User(
                username = "admin",
                email = "admin@example.com",
                password = passwordEncoder.encode("asdf1234"),
                roles = mutableSetOf(UserRole.ADMIN, UserRole.USER)
            ))
        }

        // 'performance' 테이블에서 모든 KOPIS 데이터를 가져옵니다.
        val performances = performanceRepository.findAll()

        for (perf in performances) {
            // 이미 'post' 테이블에 복사된 데이터인지 제목으로 확인합니다.
            if (postRepository.findByTitle(perf.title).isEmpty) {

                val content = """
                    ### ${perf.genre}
                    **기간:** ${perf.startDate} ~ ${perf.endDate}
                    ![포스터](${perf.posterUrl})
                """.trimIndent()

                // 'admin' 유저를 작성자로 하여 새 게시글(Post)을 만듭니다.
                val newPost = Post(
                    title = perf.title, // 공연 제목
                    content = content,  // 위에서 조합한 내용
                    author = adminUser  // 작성자
                )

                postRepository.save(newPost)
            }
        }
    }
}
