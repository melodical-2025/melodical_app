package com.melodical.backend.config;

import com.melodical.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final DaoAuthenticationProvider authProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1) disable CSRF, form-login & http-basic with lambdas
                .csrf(csrf -> csrf.disable())
                .formLogin(login -> login.disable())
                .httpBasic(basic -> basic.disable())

                // 2) use your global CorsFilter bean
                .cors(Customizer.withDefaults())

                // 3) stateless sessions (JWT only)
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4) public vs authenticated endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/auth/**", "/oauth2/**", "/api/token/**").permitAll()
                        .requestMatchers(HttpMethod.GET,"/api/musicals/fetch", "/api/musicals/rated").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/musicals/rate").authenticated()
                        .anyRequest().authenticated()
                )

                // 5) register your DAO provider for AuthenticationManager
                .authenticationProvider(authProvider)

                // 6) insert your JWT filter before Spring’s username/password filter
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}