package ru.practicum.event.service.event;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.RatingClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.dto.user.UserShortDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCircuitBreakerService {

    private final UserClient userClient;
    private final RequestClient requestClient;
    private final RatingClient ratingClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserShortByIdFallback")
    public UserShortDto getUserShortById(Long userId) {
        log.debug("Calling user-service for user: {}", userId);
        return userClient.getUserShortById(userId);
    }

    private UserShortDto getUserShortByIdFallback(Long userId, Throwable t) {
        log.warn("Circuit Breaker: user-service unavailable for user {}, fallback: {}. Error: {}",
                userId, "Unknown User", t.getMessage(), t);
        return new UserShortDto(userId, "Unknown User");
    }

    @CircuitBreaker(name = "request-service", fallbackMethod = "getConfirmedRequestsCountFallback")
    public Long getConfirmedRequestsCount(Long eventId) {
        log.debug("Calling request-service for event: {}", eventId);
        return requestClient.getConfirmedRequestsCount(eventId);
    }

    private Long getConfirmedRequestsCountFallback(Long eventId, Throwable t) {
        log.warn("Circuit Breaker: request-service unavailable for event {}, fallback: 0. Error: {}",
                eventId, t.getMessage(), t);
        return 0L;
    }

    @CircuitBreaker(name = "rating-service", fallbackMethod = "getEventRatingFallback")
    public RatingDto getEventRating(Long eventId) {
        log.debug("Calling rating-service for event: {}", eventId);
        return ratingClient.getEventRating(eventId);
    }

    private RatingDto getEventRatingFallback(Long eventId, Throwable t) {
        log.warn("Circuit Breaker: rating-service unavailable for event {}, fallback: default rating. Error: {}",
                eventId, t.getMessage(), t);
        return RatingDto.builder()
                .score(0)
                .likes(0L)
                .dislikes(0L)
                .total(0L)
                .build();
    }
}