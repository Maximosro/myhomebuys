package com.sro.myhomebuys.auth;

import com.sro.authclient.client.AuthServiceClient;
import com.sro.authclient.dto.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthServiceClient authServiceClient;

    public AuthController(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpServletResponse response) {
        try {
            AuthResponse authResponse = authServiceClient.login(email, password);
            Cookie cookie = new Cookie("auth_token", authResponse.token());
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(3600);
            response.addCookie(cookie);
            return "redirect:/";
        } catch (Exception e) {
            return "redirect:/auth/login?error";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("auth_token", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/auth/login";
    }
}
