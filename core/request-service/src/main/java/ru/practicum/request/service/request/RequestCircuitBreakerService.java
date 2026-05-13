package ru.practicum.request.service.request;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.EventClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventShortDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestCircuitBreakerService {

    private final UserClient userClient;
    private final EventClient eventClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "userExistsFallback")
    public Boolean userExists(Long userId) {
        log.debug("Calling user-service for user exists: {}", userId);
        return userClient.userExists(userId);
    }

    private Boolean userExistsFallback(Long userId, Throwable t) {
        log.warn("Circuit Breaker: user-service unavailable for user {}, fallback: true", userId);
        return true;
    }

    @CircuitBreaker(name = "event-service", fallbackMethod = "getEventByIdFallback")
    public EventShortDto getEventById(Long eventId) {
        log.debug("Calling event-service for event: {}", eventId);
        return eventClient.getEventById(eventId);
    }

    private EventShortDto getEventByIdFallback(Long eventId, Throwable t) {
        log.warn("Circuit Breaker: event-service unavailable for event {}, throwing exception", eventId);
        throw new RuntimeException("Event service unavailable");
    }

    @CircuitBreaker(name = "event-service", fallbackMethod = "eventExistsFallback")
    public Boolean eventExists(Long eventId) {
        log.debug("Calling event-service for event exists: {}", eventId);
        return eventClient.eventExists(eventId);
    }

    private Boolean eventExistsFallback(Long eventId, Throwable t) {
        log.warn("Circuit Breaker: event-service unavailable for event {}, fallback: false", eventId);
        return false;
    }
}