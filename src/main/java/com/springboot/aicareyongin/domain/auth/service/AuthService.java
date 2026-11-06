package com.springboot.aicareyongin.domain.auth.service;

import com.springboot.aicareyongin.domain.auth.dto.UserResponse;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final Clock clock;

    @Transactional(readOnly = true)
    public User getActiveUser(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원이 존재하지 않습니다."));
        if (!user.isLoginAllowed()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인할 수 없는 회원입니다.");
        }
        return user;
    }

    @Transactional
    public UserResponse agreeTerms(Long id) {
        User user = getActiveUser(id);
        user.agreeTerms(OffsetDateTime.now(clock));
        return UserResponse.from(user);
    }
}
