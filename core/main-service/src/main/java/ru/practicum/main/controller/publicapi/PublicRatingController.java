package ru.practicum.main.controller.publicapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.rating.RatingDto;
import ru.practicum.main.service.rating.RatingService;

import jakarta.validation.constraints.Min;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicRatingController {

    private final RatingService ratingService;

    @GetMapping("/events/{eventId}/rating")
    public RatingDto getEventRating(@PathVariable @Min(1) Long eventId) {
        log.info("Getting rating for event {}", eventId);
        return ratingService.getEventRating(eventId);
    }
}