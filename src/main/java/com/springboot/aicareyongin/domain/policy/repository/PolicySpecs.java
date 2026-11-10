package com.springboot.aicareyongin.domain.policy.repository;

import com.springboot.aicareyongin.domain.policy.entity.Policy;
import com.springboot.aicareyongin.domain.policy.enums.PolicyCategory;
import org.springframework.data.jpa.domain.Specification;

/**
 * 정책 목록 필터 조건. 값이 null이면 조건을 걸지 않는다.
 */
public final class PolicySpecs {

    private PolicySpecs() {
    }

    public static Specification<Policy> active() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Policy> categoryEquals(PolicyCategory category) {
        if (category == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** 대상 학년 범위(min~max)에 grade가 포함되는 정책. 범위가 비어 있으면 전 학년 대상으로 본다. */
    public static Specification<Policy> targetsGrade(Integer grade) {
        if (grade == null) {
            return Specification.unrestricted();
        }
        short g = grade.shortValue();
        return (root, query, cb) -> cb.and(
                cb.or(cb.isNull(root.get("targetGradeMin")), cb.le(root.get("targetGradeMin"), g)),
                cb.or(cb.isNull(root.get("targetGradeMax")), cb.ge(root.get("targetGradeMax"), g)));
    }
}
