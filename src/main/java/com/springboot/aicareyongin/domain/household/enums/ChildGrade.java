package com.springboot.aicareyongin.domain.household.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ChildGrade {
    KINDERGARTEN_5("유치5세", 1),
    KINDERGARTEN_6("유치6세", 2),
    KINDERGARTEN_7("유치7세", 3),
    ELEMENTARY_1("초1", 4),
    ELEMENTARY_2("초2", 5),
    ELEMENTARY_3("초3", 6),
    ELEMENTARY_4("초4", 7),
    ELEMENTARY_5("초5", 8),
    ELEMENTARY_6("초6", 9);

    private final String label;
    private final short gradeOrder;

    ChildGrade(String label, int gradeOrder) {
        this.label = label;
        this.gradeOrder = (short) gradeOrder;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    public short getGradeOrder() {
        return gradeOrder;
    }

    @JsonCreator
    public static ChildGrade fromLabel(String label) {
        return Arrays.stream(values()).filter(grade -> grade.label.equals(label)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 학년입니다: " + label));
    }

    public static ChildGrade fromGradeOrder(short order) {
        return Arrays.stream(values()).filter(grade -> grade.gradeOrder == order).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 학년 코드입니다: " + order));
    }
}
