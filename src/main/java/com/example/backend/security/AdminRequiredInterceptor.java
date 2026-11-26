package com.example.backend.security;

import com.example.backend.model.UserInfo;
import com.example.backend.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 管理端角色校验。必须注册在 JwtAuthInterceptor 之后：先由它验 JWT 注入 userId，这里再查库取 role。
 * 每次查库是为了角色变更即时生效，不用等 token 过期。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminRequiredInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        Object userId = request.getAttribute("userId");
        UserInfo user = userId == null ? null : userRepository.findById(userId.toString()).orElse(null);
        if (user == null) {
            // 用户已删除但 token 还没过期
            return reject(response, HttpStatus.UNAUTHORIZED, "未登录或凭证已过期");
        }
        if (!UserInfo.ROLE_ADMIN.equals(user.getRole())) {
            log.info("[Auth] 非管理员访问管理端 userId={} {} {}", user.getId(),
                    request.getMethod(), request.getRequestURI());
            return reject(response, HttpStatus.FORBIDDEN, "需要管理员权限");
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
        return false;
    }
}
