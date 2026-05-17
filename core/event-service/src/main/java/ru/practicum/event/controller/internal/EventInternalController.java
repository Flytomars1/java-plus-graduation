package ru.practicum.event.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.api.EventInternalApi;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.event.service.event.EventService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EventInternalController implements EventInternalApi {

    private final EventService eventService;

    @Override
    public Boolean eventExists(Long eventId) {
        log.debug("Internal API: check if event {} exists", eventId);
        return eventService.existsById(eventId);
    }

    @Override
    public EventShortDto getEventById(Long eventId) {
        log.debug("Internal API: get event by id {}", eventId);
        return eventService.getEventShortById(eventId);
    }

    @Override
    public Boolean isEventPublished(Long eventId) {
        log.debug("Internal API: check if event {} is published", eventId);
        return eventService.isEventPublished(eventId);
    }
}