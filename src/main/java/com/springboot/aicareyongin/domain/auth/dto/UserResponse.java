package com.springboot.aicareyongin.domain.auth.dto;

import com.springboot.aicareyongin.domain.user.entity.User;

public record UserResponse(Long id, String nickname, String email, boolean termsAgreementRequired) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNickname(), user.getEmail(),
                user.getTermsAgreedAt() == null || user.getPrivacyAgreedAt() == null);
    }
}
