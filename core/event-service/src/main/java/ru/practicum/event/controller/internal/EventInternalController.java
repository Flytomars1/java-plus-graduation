package ru.practicum.event.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.api.EventInternalApi;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.service.event.EventCircuitBreakerService;
import ru.practicum.exception.NotFoundException;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EventInternalController implements EventInternalApi {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final EventCircuitBreakerService circuitBreakerService;

    @Override
    public Boolean eventExists(Long eventId) {
        log.debug("Internal API: check if event {} exists", eventId);
        return eventRepository.existsById(eventId);
    }

    @Override
    public EventShortDto getEventById(Long eventId) {
        log.debug("Internal API: get event by id {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + eventId));

        EventShortDto dto = eventMapper.toShort(event, 0L);
        dto.setState(event.getState().name());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());

        if (event.getInitiatorId() != null) {
            UserShortDto initiator = circuitBreakerService.getUserShortById(event.getInitiatorId());
            dto.setInitiator(initiator);
        }

        return dto;
    }

    @Override
    public Boolean isEventPublished(Long eventId) {
        log.debug("Internal API: check if event {} is published", eventId);
        return eventRepository.findById(eventId)
                .map(event -> event.getState().name().equals("PUBLISHED"))
                .orElse(false);
    }
}