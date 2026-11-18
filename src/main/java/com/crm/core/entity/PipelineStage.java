package com.crm.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "pipeline_stages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PipelineStage extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false, updatable = false)
    private UUID pipelineId;

    @Column(nullable = false)
    private String name;

    /** Порядок этапа в воронке, начиная с 0. */
    @Column(nullable = false)
    private int position;

    /** Удалённый этап: скрыт из воронки, но остаётся в истории лидов. */
    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
