package ru.practicum.api;

import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.ParticipationRequestDto;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;

import java.util.List;
import java.util.Map;

public interface RequestInternalApi {

    @GetMapping("/internal/requests/count/{eventId}")
    Long getConfirmedRequestsCount(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/requests/counts/batch")
    Map<Long, Long> getConfirmedRequestsCounts(@RequestBody List<Long> eventIds);

    @GetMapping("/internal/requests/event/{eventId}")
    List<ParticipationRequestDto> getRequestsByEventId(@PathVariable("eventId") Long eventId);

    @PutMapping("/internal/requests/event/{eventId}/status")
    EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable("eventId") Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request);
}