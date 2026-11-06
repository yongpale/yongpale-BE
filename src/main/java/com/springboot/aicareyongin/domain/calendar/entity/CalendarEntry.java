package com.springboot.aicareyongin.domain.calendar.entity;

import com.springboot.aicareyongin.domain.policy.entity.PolicySchedule;
import com.springboot.aicareyongin.domain.user.entity.User;
import com.springboot.aicareyongin.global.entity.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "calendar_entries", indexes = {
        @Index(name = "idx_calendar_entries_active", columnList = "schedule_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "schedule_id"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CalendarEntry extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PolicySchedule schedule;

    @Builder.Default
    @ColumnDefault("3")
    @Column(name = "remind_days_before", nullable = false)
    private Short remindDaysBefore = (short) 3;

    @Builder.Default
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 10)
    private String status = "ACTIVE";

    @Column(name = "last_notified_at", columnDefinition = "datetime(6)")
    private OffsetDateTime lastNotifiedAt;
}
