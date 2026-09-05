package com.crm.core.repository;

import com.crm.core.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findAllByGroupIdOrderByStartTimeAsc(UUID groupId);

    @Query("SELECT l FROM Lesson l " +
            "JOIN Subscription s ON l.groupId = s.groupId " +
            "JOIN Person p ON p.id = s.studentId " +
            "WHERE p.messengerId = :chatId AND l.startTime >= :now " +
            "ORDER BY l.startTime ASC")
    List<Lesson> findUpcomingLessonsByChatId(@Param("chatId") String chatId, @Param("now") LocalDateTime now);
}
