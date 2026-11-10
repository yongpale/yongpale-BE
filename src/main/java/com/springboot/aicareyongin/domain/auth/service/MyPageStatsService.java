package com.springboot.aicareyongin.domain.auth.service;

import com.springboot.aicareyongin.domain.auth.dto.MyPageCounts;
import org.springframework.stereotype.Service;

@Service
public class MyPageStatsService {
    public MyPageCounts getCounts(Long userId) {
        // TODO: 정책 저장 담당 기능에서 userId 기준 저장 개수를 조회한다.
        int savedPolicyCount = 0;
        // TODO: 신청 일정 담당 기능에서 userId 기준 일정 개수를 조회한다.
        int applicationScheduleCount = 0;
        // TODO: 맞춤 혜택 담당 기능에서 userId 기준 혜택 개수를 조회한다.
        int matchedBenefitCount = 0;
        return new MyPageCounts(savedPolicyCount, applicationScheduleCount, matchedBenefitCount);
    }
}
