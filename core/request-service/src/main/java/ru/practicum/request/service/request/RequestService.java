package ru.practicum.request.service.request;

import ru.practicum.dto.event.ParticipationRequestDto;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    Long getConfirmedRequestsCount(Long eventId);

    List<ParticipationRequestDto> getRequestsByEventId(Long eventId);

    EventRequestStatusUpdateResult updateRequestStatus(Long eventId, EventRequestStatusUpdateRequest request);
}