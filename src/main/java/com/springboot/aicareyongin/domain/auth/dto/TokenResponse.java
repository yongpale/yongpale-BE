package com.springboot.aicareyongin.domain.auth.dto;

public record TokenResponse(String tokenType, String accessToken, long expiresIn, UserResponse user,
                            String refreshToken, long refreshExpiresIn) {
}
