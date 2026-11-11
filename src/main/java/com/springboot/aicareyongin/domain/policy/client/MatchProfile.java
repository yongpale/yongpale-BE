package com.springboot.aicareyongin.domain.policy.client;

import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import java.util.List;

/**
 * AI 서버에 보내는 가구 프로필. 백엔드가 정한 형식으로 매칭·요약·돌봄 공백 분석에 공통으로 쓴다.
 *
 * @param guName       거주 구 (예: 기흥구)
 * @param dongName     거주 읍·면·동 (예: 구갈동, 행정동)
 * @param childCount   자녀 수
 * @param childGrades  자녀별 학년 ("초1"~"초6", "유치5세"~"유치7세"), 자녀 등록 순서
 * @param dualIncome   맞벌이 여부
 * @param singleParent 한부모 여부
 * @param multiChild   다자녀 여부
 * @param careTime     하교 후 돌봄 필요 시간. 입력하지 않았으면 null
 */
public record MatchProfile(
        String guName,
        String dongName,
        int childCount,
        List<ChildGrade> childGrades,
        boolean dualIncome,
        boolean singleParent,
        boolean multiChild,
        CareTime careTime) {

    /**
     * @param start 시작 시각 ("13:00")
     * @param end   종료 시각 ("18:00")
     */
    public record CareTime(String start, String end) {
    }
}
