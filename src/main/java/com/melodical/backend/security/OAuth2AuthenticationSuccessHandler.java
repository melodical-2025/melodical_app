package com.melodical.backend.security;

import com.melodical.backend.config.JwtTokenProvider;
import com.melodical.backend.entity.User;
import com.melodical.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isEmpty()) {
            user = User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .password("")
                    .build();
            user = userRepository.save(user);
        } else {
            user = userOptional.get();
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getId());

        String redirectUrl = String.format("http://localhost:3000/oauth2/redirect?token=%s", token);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
