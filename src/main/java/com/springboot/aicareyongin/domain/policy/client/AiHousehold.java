package com.springboot.aicareyongin.domain.policy.client;

import com.springboot.aicareyongin.domain.household.enums.ChildGrade;
import java.util.List;

/**
 * AI 서버에 보낼 프로필과, 백엔드에서만 쓰는 자녀 목록(ID 포함).
 * children의 순서는 profile.childGrades 순서와 같다 (AI 서버의 child_index).
 */
public record AiHousehold(MatchProfile profile, List<ChildInfo> children) {

    public record ChildInfo(Long childId, String nickname, ChildGrade grade) {

        /** 돌봄 공백 분석·시설 데이터는 초등학생 대상 */
        public boolean isElementary() {
            return grade.getGradeOrder() >= ChildGrade.ELEMENTARY_1.getGradeOrder();
        }
    }
}
