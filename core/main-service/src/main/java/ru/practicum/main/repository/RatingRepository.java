package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.main.model.EventRating;

import java.util.Optional;

public interface RatingRepository extends JpaRepository<EventRating, Long> {
    Optional<EventRating> findByUserIdAndEventId(Long userId, Long eventId);

    long countByEventIdAndIsLikeTrue(Long eventId);
    long countByEventIdAndIsLikeFalse(Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}