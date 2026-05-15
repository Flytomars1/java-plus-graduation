package ru.practicum.event.mapper;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Event toEntity(NewEventDto dto, Long initiatorId) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .title(dto.getTitle())
                .categoryId(dto.getCategory())
                .eventDate(dto.getEventDate())
                .locationLat(dto.getLocation().getLat())
                .locationLon(dto.getLocation().getLon())
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .initiatorId(initiatorId)
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
    }

    public static EventShortDto toShort(Event e, CategoryDto catDto, UserShortDto userDto,
                                        Long views, Long confirmedRequests, RatingDto rating) {
        EventShortDto dto = new EventShortDto();
        dto.setId(e.getId());
        dto.setAnnotation(e.getAnnotation());
        dto.setCategory(catDto);
        dto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0L);
        dto.setEventDate(e.getEventDate() != null ? e.getEventDate().format(FORMATTER) : null);
        dto.setInitiator(userDto);
        dto.setPaid(e.getPaid());
        dto.setTitle(e.getTitle());
        dto.setViews(views != null ? views : 0L);
        dto.setRating(rating);
        return dto;
    }

    public static EventShortDto toShort(Event e, CategoryDto catDto, UserShortDto userDto,
                                        Long views, Long confirmedRequests) {
        return toShort(e, catDto, userDto, views, confirmedRequests, null);
    }

    public static EventShortDto toShort(Event e, Long views) {
        EventShortDto dto = new EventShortDto();
        dto.setId(e.getId());
        dto.setAnnotation(e.getAnnotation());
        dto.setConfirmedRequests(0L);
        dto.setEventDate(e.getEventDate() != null ? e.getEventDate().format(FORMATTER) : null);
        dto.setPaid(e.getPaid());
        dto.setTitle(e.getTitle());
        dto.setViews(views != null ? views : 0L);
        return dto;
    }

    public static EventFullDto toFull(Event e, CategoryDto catDto, UserShortDto userDto,
                                      Long views, Long confirmedRequests, RatingDto rating) {
        LocationDto loc = new LocationDto(e.getLocationLat(), e.getLocationLon());

        return EventFullDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .description(e.getDescription())
                .category(catDto)
                .confirmedRequests(confirmedRequests != null ? confirmedRequests : 0L)
                .eventDate(e.getEventDate() != null ? e.getEventDate().format(FORMATTER) : null)
                .initiator(userDto)
                .location(loc)
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .requestModeration(e.getRequestModeration())
                .state(e.getState().name())
                .createdOn(e.getCreatedOn() != null ? e.getCreatedOn().format(FORMATTER) : null)
                .publishedOn(e.getPublishedOn() != null ? e.getPublishedOn().format(FORMATTER) : null)
                .title(e.getTitle())
                .views(views != null ? views : 0L)
                .rating(rating)
                .build();
    }

    public static EventFullDto toFull(Event e, CategoryDto catDto, UserShortDto userDto,
                                      Long views, Long confirmedRequests) {
        return toFull(e, catDto, userDto, views, confirmedRequests, null);
    }
}