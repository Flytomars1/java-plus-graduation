package ru.practicum.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.event.EventShortDto;

public interface EventInternalApi {

    @GetMapping("/internal/events/{eventId}/exists")
    Boolean eventExists(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events/{eventId}")
    EventShortDto getEventById(@PathVariable("eventId") Long eventId);

    @GetMapping("/internal/events/{eventId}/is-published")
    Boolean isEventPublished(@PathVariable("eventId") Long eventId);
}