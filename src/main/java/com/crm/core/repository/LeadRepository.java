package com.crm.core.repository;

import com.crm.core.entity.Lead;
import com.crm.core.entity.enums.LeadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Фильтр по организации добавляет Hibernate (@TenantId), здесь его указывать не нужно. */
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    /** Поиск дубля — в том числе среди архивных: повторное обращение находит прежнюю карточку. */
    Optional<Lead> findFirstByPhoneNumber(String phoneNumber);

    Optional<Lead> findFirstByMessengerId(String messengerId);

    boolean existsByStageId(UUID stageId);

    /**
     * Фильтры необязательны: null означает «любой». query ищет по имени, телефону и мессенджеру;
     * пустая строка — без поиска (null для строки Hibernate передаёт в PostgreSQL как bytea).
     */
    @Query("""
            SELECT l FROM Lead l
            WHERE l.archived = :archived
              AND (:pipelineId IS NULL OR l.pipelineId = :pipelineId)
              AND (:stageId IS NULL OR l.stageId = :stageId)
              AND (:status IS NULL OR l.status = :status)
              AND (:managerId IS NULL OR l.managerId = :managerId)
              AND (:query = ''
                   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR l.phoneNumber LIKE CONCAT('%', :query, '%')
                   OR LOWER(l.messengerId) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY l.createdAt DESC
            """)
    List<Lead> search(@Param("archived") boolean archived,
                      @Param("pipelineId") UUID pipelineId,
                      @Param("stageId") UUID stageId,
                      @Param("status") LeadStatus status,
                      @Param("managerId") UUID managerId,
                      @Param("query") String query);

    /** Перенос лидов при удалении этапа. Оба этапа уже проверены на принадлежность организации. */
    @Query("UPDATE Lead l SET l.stageId = :to WHERE l.stageId = :from")
    @Modifying
    int moveAllToStage(@Param("from") UUID from, @Param("to") UUID to);
}
