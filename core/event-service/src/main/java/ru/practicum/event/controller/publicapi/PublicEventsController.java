package ru.practicum.event.controller.publicapi;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.event.service.event.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@Validated
public class PublicEventsController {

    private final EventService service;

    @GetMapping
    public List<EventShortDto> search(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "EVENT_DATE") String sort,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Min(1) Integer size,
            HttpServletRequest req
    ) {
        return service.searchPublic(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, req.getRequestURI(), req.getRemoteAddr());
    }

    @GetMapping("/{id}")
    public EventFullDto getById(
            @PathVariable Long id,
            @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
            HttpServletRequest request) {
        return service.getPublicById(id, request.getRequestURI(), request.getRemoteAddr(), userId);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        return service.getRecommendations(userId, from, size);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @RequestHeader("X-EWM-USER-ID") Long userId,
            @PathVariable Long eventId) {
        service.likeEvent(userId, eventId);
    }
}