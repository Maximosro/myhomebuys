package com.sro.myhomebuys.config;

import com.nimbusds.jwt.JWTClaimsSet;
import com.sro.authclient.JwtTokenProvider;
import com.sro.authclient.AuthUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CookieJwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public CookieJwtFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractTokenFromCookie(request);
        if (token != null) {
            try {
                JWTClaimsSet claims = tokenProvider.validateToken(token);
                String email = claims.getSubject();
                String rolesStr = (String) claims.getClaim("roles");
                List<GrantedAuthority> authorities = (rolesStr != null)
                        ? Arrays.stream(rolesStr.split(","))
                                .map(String::trim)
                                .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                                .toList()
                        : List.of();

                AuthUser authUser = new AuthUser(email, authorities, token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(authUser, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                clearAuthCookie(response);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("auth_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void clearAuthCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("auth_token", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
