package com.sro.myhomebuys.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public AuthController(RestTemplate restTemplate,
                          @Value("${auth.service.url:http://localhost:8091}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
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
            Map<String, String> body = Map.of("email", email, "password", password);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> authResponse = restTemplate.postForEntity(
                    authServiceUrl + "/auth/login", request, Map.class);

            if (authResponse.getStatusCode().is2xxSuccessful() && authResponse.getBody() != null) {
                String token = (String) authResponse.getBody().get("token");
                Cookie cookie = new Cookie("auth_token", token);
                cookie.setPath("/");
                cookie.setHttpOnly(true);
                cookie.setMaxAge(3600);
                response.addCookie(cookie);
                return "redirect:/";
            }
        } catch (Exception e) {
            // Fall through to error redirect
        }
        return "redirect:/auth/login?error";
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
