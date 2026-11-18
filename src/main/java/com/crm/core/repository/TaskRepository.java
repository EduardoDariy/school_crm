package com.crm.core.repository;

import com.crm.core.entity.CrmTask;
import com.crm.core.entity.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<CrmTask, UUID> {

    /** Фильтры необязательны: null означает «любой». */
    @Query("""
            SELECT t FROM CrmTask t
            WHERE (:assigneeId IS NULL OR t.assigneeId = :assigneeId)
              AND (:status IS NULL OR t.status = :status)
              AND (:leadId IS NULL OR t.leadId = :leadId)
              AND (:personId IS NULL OR t.personId = :personId)
            ORDER BY t.dueAt ASC
            """)
    List<CrmTask> search(@Param("assigneeId") UUID assigneeId,
                         @Param("status") TaskStatus status,
                         @Param("leadId") UUID leadId,
                         @Param("personId") UUID personId);

    long countByAssigneeIdAndStatusAndDueAtBefore(UUID assigneeId, TaskStatus status, LocalDateTime before);

    long countByAssigneeIdAndStatusAndDueAtBetween(UUID assigneeId, TaskStatus status,
                                                   LocalDateTime from, LocalDateTime to);

    long countByAssigneeIdAndStatus(UUID assigneeId, TaskStatus status);
}
