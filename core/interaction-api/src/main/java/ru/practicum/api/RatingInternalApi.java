package ru.practicum.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.rating.RatingDto;

public interface RatingInternalApi {

    @GetMapping("/internal/ratings/event/{eventId}")
    RatingDto getEventRating(@PathVariable("eventId") Long eventId);
}