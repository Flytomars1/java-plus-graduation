package ru.practicum.rating.service.rating;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.rating.RateEventRequest;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.rating.mapper.RatingMapper;
import ru.practicum.rating.model.EventRating;
import ru.practicum.rating.repository.RatingRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingCircuitBreakerService circuitBreakerService;

    @Override
    @Transactional
    public void rateEvent(Long userId, RateEventRequest request) {
        log.debug("User {} rating event {} as {}", userId, request.getEventId(), request.getIsLike());

        if (!Boolean.TRUE.equals(circuitBreakerService.userExists(userId))) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        EventShortDto event;
        try {
            event = circuitBreakerService.getEventById(request.getEventId());
        } catch (Exception e) {
            throw new NotFoundException("Event with id=" + request.getEventId() + " was not found");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Cannot rate own event");
        }

        if (!"PUBLISHED".equals(event.getState())) {
            throw new ConflictException("Cannot rate unpublished event");
        }

        Long confirmedRequests = circuitBreakerService.getConfirmedRequestsCount(request.getEventId());
        if (confirmedRequests == 0) {
            throw new ConflictException("Must have participated in event to rate it");
        }

        EventRating existingRating = ratingRepository.findByUserIdAndEventId(userId, request.getEventId())
                .orElse(null);

        EventRating rating;
        if (existingRating != null) {
            existingRating.setIsLike(request.getIsLike());
            existingRating.setCreated(LocalDateTime.now());
            rating = existingRating;
            log.info("User {} updated rating for event {} to {}",
                    userId, request.getEventId(), request.getIsLike() ? "like" : "dislike");
        } else {
            rating = EventRating.builder()
                    .userId(userId)
                    .eventId(request.getEventId())
                    .isLike(request.getIsLike())
                    .created(LocalDateTime.now())
                    .build();
            log.info("User {} rated event {} as {}",
                    userId, request.getEventId(), request.getIsLike() ? "like" : "dislike");
        }

        ratingRepository.save(rating);
    }

    @Override
    public RatingDto getEventRating(Long eventId) {
        if (!Boolean.TRUE.equals(circuitBreakerService.eventExists(eventId))) {
            throw new NotFoundException("Event not found");
        }

        long likes = ratingRepository.countByEventIdAndIsLikeTrue(eventId);
        long dislikes = ratingRepository.countByEventIdAndIsLikeFalse(eventId);
        long total = likes + dislikes;

        if (total == 0) {
            return RatingDto.builder()
                    .score(0)
                    .likes(0L)
                    .dislikes(0L)
                    .total(0L)
                    .build();
        }

        return RatingMapper.toRatingDto(eventId, likes, dislikes);
    }

    @Override
    @Transactional
    public void deleteRating(Long userId, Long eventId) {
        EventRating rating = ratingRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new NotFoundException("Rating not found"));

        if (!rating.getUserId().equals(userId)) {
            throw new ConflictException("Cannot delete other user's rating");
        }

        ratingRepository.delete(rating);
    }

    @Override
    public Boolean getUserRatingForEvent(Long userId, Long eventId) {
        log.debug("Getting user {} rating for event {}", userId, eventId);

        return ratingRepository.findByUserIdAndEventId(userId, eventId)
                .map(EventRating::getIsLike)
                .orElse(null);
    }

    @Override
    public Map<Long, RatingDto> getEventRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, RatingDto> result = new HashMap<>();
        for (Long eventId : eventIds) {
            result.put(eventId, getEventRating(eventId));
        }
        return result;
    }
}