package ru.practicum.recommendation.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.recommendation.analyzer.model.EventSimilarityEntity;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    Optional<EventSimilarityEntity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT e FROM EventSimilarityEntity e WHERE e.eventA = :eventId OR e.eventB = :eventId")
    List<EventSimilarityEntity> findAllByEventId(@Param("eventId") Long eventId);

    @Query("SELECT e FROM EventSimilarityEntity e WHERE (e.eventA = :eventId OR e.eventB = :eventId) AND e.score > :minScore ORDER BY e.score DESC")
    List<EventSimilarityEntity> findSimilarEvents(@Param("eventId") Long eventId, @Param("minScore") Double minScore);

    @Query("SELECT e FROM EventSimilarityEntity e WHERE e.eventA IN :eventIds OR e.eventB IN :eventIds")
    List<EventSimilarityEntity> findAllByEventIds(@Param("eventIds") List<Long> eventIds);
}