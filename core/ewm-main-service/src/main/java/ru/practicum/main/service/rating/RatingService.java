package ru.practicum.main.service.rating;

import ru.practicum.main.dto.rating.RatingDto;
import ru.practicum.main.dto.rating.RateEventRequest;


public interface RatingService {
    void rateEvent(Long userId, RateEventRequest request);

    void deleteRating(Long userId, Long ratingId);

    RatingDto getEventRating(Long eventId);

    Boolean getUserRatingForEvent(Long userId, Long eventId);
}