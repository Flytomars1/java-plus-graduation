package ru.practicum.rating.controller.publicapi;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.rating.service.rating.RatingService;

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