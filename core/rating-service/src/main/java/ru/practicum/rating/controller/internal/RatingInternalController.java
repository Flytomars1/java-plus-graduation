package ru.practicum.rating.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.api.RatingInternalApi;
import ru.practicum.dto.rating.EventRatingDto;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.rating.service.rating.RatingService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RatingInternalController implements RatingInternalApi {

    private final RatingService ratingService;

    @Override
    public RatingDto getEventRating(Long eventId) {
        log.debug("Internal API: get rating for event {}", eventId);
        return ratingService.getEventRating(eventId);
    }
}