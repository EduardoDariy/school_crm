package com.crm.core.entity;

import com.crm.core.entity.enums.LostReasonMode;
import jakarta.persistence.*;
import lombok.*;

/** Воронка продаж. Этапы и причины проигрыша полностью настраивает администратор школы. */
@Entity
@Table(name = "pipelines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pipeline extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Сюда попадают лиды, для которых воронка не указана явно. */
    @Column(name = "is_default", nullable = false)
    private boolean defaultPipeline;

    @Enumerated(EnumType.STRING)
    @Column(name = "lost_reason_mode", nullable = false)
    private LostReasonMode lostReasonMode;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
