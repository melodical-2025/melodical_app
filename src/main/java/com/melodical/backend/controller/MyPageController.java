package com.melodical.backend.controller;

import com.melodical.backend.dto.SessionUser;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyPageController {

    private final HttpSession httpSession;

    public MyPageController(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    @GetMapping("/mypage")
    public String myPage() {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");

        if (user == null) {
            return "로그인이 필요합니다.";
        }

        return "환영합니다, " + user.getName() + "님 (" + user.getEmail() + ")";
    }
}
