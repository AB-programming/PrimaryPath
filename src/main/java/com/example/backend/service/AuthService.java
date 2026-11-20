package com.example.backend.service;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.RegisterRequest;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.UserInfo;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse register(RegisterRequest req) {
        validateRegisterRequest(req);

        UserInfo user = UserInfo.builder()
                .id(UUID.randomUUID().toString())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .build();
        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            log.info("[Auth] register conflict username={}", req.username());
            throw BusinessException.conflict("用户名已被占用");
        }
        log.info("[Auth] registered username={}, id={}", user.getUsername(), user.getId());
        return new AuthResponse(jwtTokenProvider.generateToken(user), user);
    }

    public AuthResponse login(LoginRequest req) {
        Optional<UserInfo> userOpt = req.username() == null
                ? Optional.empty()
                : userRepository.findByUsername(req.username());
        if (userOpt.isEmpty() || req.password() == null
                || !passwordEncoder.matches(req.password(), userOpt.get().getPasswordHash())) {
            // 用户不存在与密码错误统一提示，避免用户名枚举
            log.info("[Auth] login failed username={}", req.username());
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        UserInfo user = userOpt.get();
        log.info("[Auth] login ok username={}", user.getUsername());
        return new AuthResponse(jwtTokenProvider.generateToken(user), user);
    }

    private void validateRegisterRequest(RegisterRequest req) {
        if (req.username() == null || !USERNAME_PATTERN.matcher(req.username()).matches()) {
            throw BusinessException.badRequest("用户名需为 3-32 位字母、数字或下划线");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw BusinessException.badRequest("密码至少 6 位");
        }
    }
}
