package org.example.graduation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.graduation.common.BusinessException;
import org.example.graduation.common.Result;
import org.example.graduation.dto.LoginDTO;
import org.example.graduation.security.JwtTokenProvider;
import org.example.graduation.service.AuthService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        return Result.success(authService.login(loginDTO, getClientIp(request)));
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success();
    }

    @GetMapping("/me")
    public Result<?> me(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return Result.success(authService.getCurrentUserInfo(userId));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearer) || !bearer.startsWith("Bearer ")) {
            throw new BusinessException(401, "未认证");
        }
        return jwtTokenProvider.getUserIdFromToken(bearer.substring(7));
    }
}