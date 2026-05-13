package ru.practicum.request.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.ParticipationRequestDto;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.request.service.request.RequestService;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestInternalController {

    private final RequestService requestService;

    @GetMapping("/count/{eventId}")
    public Long getConfirmedRequestsCount(@PathVariable Long eventId) {
        log.debug("Internal API: get confirmed requests count for event {}", eventId);
        return requestService.getConfirmedRequestsCount(eventId);
    }

    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getRequestsByEventId(@PathVariable Long eventId) {
        log.debug("Internal API: get all requests for event {}", eventId);
        return requestService.getRequestsByEventId(eventId);
    }

    @PutMapping("/event/{eventId}/status")
    public EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request) {
        log.debug("Internal API: update request statuses for event {}", eventId);
        return requestService.updateRequestStatus(eventId, request);
    }
}