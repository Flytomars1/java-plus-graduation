package ru.practicum.event.service.event;

import ru.practicum.dto.event.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto create(Long userId, NewEventDto dto);

    EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest dto);

    EventFullDto getUserEvent(Long userId, Long eventId);

    List<EventShortDto> getUserEvents(Long userId, int from, int size);

    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto);

    List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                     LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                     Boolean onlyAvailable, String sort, int from, int size, String requestUri, String ip);

    EventFullDto getPublicById(Long eventId, String requestUri, String ip, Long userId);

    List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                   LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto);

    boolean existsById(Long eventId);

    EventShortDto getEventShortById(Long eventId);

    boolean isEventPublished(Long eventId);

    List<EventShortDto> getRecommendations(Long userId, int from, int size);
    void likeEvent(Long userId, Long eventId);
}