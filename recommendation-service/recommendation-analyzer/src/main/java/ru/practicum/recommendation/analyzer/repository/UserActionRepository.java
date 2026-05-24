package ru.practicum.recommendation.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.recommendation.analyzer.model.UserActionEntity;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserActionEntity, Long> {

    Optional<UserActionEntity> findTopByUserIdAndEventIdOrderByWeightDesc(Long userId, Long eventId);

    List<UserActionEntity> findByUserId(Long userId);

    List<UserActionEntity> findByUserIdAndEventIdIn(Long userId, List<Long> eventIds);

    @Query("SELECT DISTINCT u.eventId FROM UserActionEntity u WHERE u.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT u.eventId, SUM(u.weight) FROM UserActionEntity u WHERE u.eventId IN :eventIds GROUP BY u.eventId")
    List<Object[]> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);
}