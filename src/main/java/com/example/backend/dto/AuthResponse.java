package com.example.backend.dto;

import com.example.backend.model.UserInfo;

public record AuthResponse(String token, UserInfo user) {
}
