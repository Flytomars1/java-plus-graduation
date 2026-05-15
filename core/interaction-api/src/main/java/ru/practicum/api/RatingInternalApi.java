package ru.practicum.api;

import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.rating.RatingDto;

import java.util.List;
import java.util.Map;

public interface RatingInternalApi {

    @GetMapping("/internal/ratings/event/{eventId}")
    RatingDto getEventRating(@PathVariable("eventId") Long eventId);

    @PostMapping("/internal/ratings/events/batch")
    Map<Long, RatingDto> getEventRatings(@RequestBody List<Long> eventIds);
}