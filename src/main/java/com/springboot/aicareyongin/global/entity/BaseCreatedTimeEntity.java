package com.springboot.aicareyongin.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@MappedSuperclass
public abstract class BaseCreatedTimeEntity {

    @CreationTimestamp
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;
}
