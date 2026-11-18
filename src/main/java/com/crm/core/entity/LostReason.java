package com.crm.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/** Причина проигрыша лида. Не удаляется (на неё ссылаются лиды), а деактивируется. */
@Entity
@Table(name = "lost_reasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostReason extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false, updatable = false)
    private UUID pipelineId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int position;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
