package ru.practicum.main.controller.privateapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.rating.RateEventRequest;
import ru.practicum.main.service.rating.RatingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/users/{userId}/ratings")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateRatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void rateEvent(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody RateEventRequest request
    ) {
        log.info("User {} rating event {} as {}", userId, request.getEventId(), request.getIsLike());
        ratingService.rateEvent(userId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(
            @PathVariable @Min(1) Long userId,
            @RequestParam @Min(1) Long eventId) {
        log.info("User {} deleting rating for event {}", userId, eventId);
        ratingService.deleteRating(userId, eventId);
    }

    @GetMapping("/{eventId}")
    public Boolean getUserRatingForEvent(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long eventId
    ) {
        log.info("Getting user {} rating for event {}", userId, eventId);
        return ratingService.getUserRatingForEvent(userId, eventId);
    }
}