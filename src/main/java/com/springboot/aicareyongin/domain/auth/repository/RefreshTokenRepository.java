package com.springboot.aicareyongin.domain.auth.repository;

import com.springboot.aicareyongin.domain.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
}
