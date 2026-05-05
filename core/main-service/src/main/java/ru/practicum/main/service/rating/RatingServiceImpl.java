package ru.practicum.main.service.rating;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.rating.RatingDto;
import ru.practicum.main.dto.rating.RateEventRequest;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.RatingMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.*;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    @Override
    @Transactional
    public void rateEvent(Long userId, RateEventRequest request) {
        log.debug("User {} rating event {} as {}", userId, request.getEventId(), request.getIsLike());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new NotFoundException("Event with id=" + request.getEventId() + " was not found"));

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Cannot rate own event");
        }

        //if (event.getEventDate().isAfter(LocalDateTime.now())) {
        //    throw new ConflictException("Cannot rate future event");
        //}

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot rate unpublished event");
        }

        boolean participated = requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, request.getEventId(), RequestStatus.CONFIRMED);

        if (!participated) {
            throw new ConflictException("Must have participated in event to rate it");
        }

        EventRating existingRating = ratingRepository.findByUserIdAndEventId(userId, request.getEventId())
                .orElse(null);

        EventRating rating;
        if (existingRating != null) {
            existingRating.setIsLike(request.getIsLike());
            existingRating.setCreated(LocalDateTime.now());
            rating = existingRating;
            log.info("User {} updated rating for event {} to {}",
                    userId, request.getEventId(), request.getIsLike() ? "like" : "dislike");
        } else {
            rating = EventRating.builder()
                    .user(user)
                    .event(event)
                    .isLike(request.getIsLike())
                    .created(LocalDateTime.now())
                    .build();
            log.info("User {} rated event {} as {}",
                    userId, request.getEventId(), request.getIsLike() ? "like" : "dislike");
        }

        ratingRepository.save(rating);
    }

    @Override
    public RatingDto getEventRating(Long eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        long likes = ratingRepository.countByEventIdAndIsLikeTrue(eventId);
        long dislikes = ratingRepository.countByEventIdAndIsLikeFalse(eventId);
        long total = likes + dislikes;

        if (total == 0) {
            return RatingDto.builder()
                    .score(null)
                    .likes(0L)
                    .dislikes(0L)
                    .total(0L)
                    .build();
        }

        return RatingMapper.toRatingDto(eventId, likes, dislikes);
    }

    @Override
    @Transactional
    public void deleteRating(Long userId, Long eventId) {
        EventRating rating = ratingRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new NotFoundException("Rating not found"));

        if (!rating.getUser().getId().equals(userId)) {
            throw new ConflictException("Cannot delete other user's rating");
        }

        ratingRepository.delete(rating);
    }

    @Override
    public Boolean getUserRatingForEvent(Long userId, Long eventId) {
        log.debug("Getting user {} rating for event {}", userId, eventId);

        return ratingRepository.findByUserIdAndEventId(userId, eventId)
                .map(EventRating::getIsLike)
                .orElse(null);
    }
}