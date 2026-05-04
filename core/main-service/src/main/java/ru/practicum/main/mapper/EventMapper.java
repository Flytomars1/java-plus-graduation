package ru.practicum.main.mapper;

import ru.practicum.main.dto.rating.RatingDto;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.dto.event.*;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.dto.user.UserShortDto;

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
                                        long views, long confirmedRequests, RatingDto rating) {
        return new EventShortDto(
                e.getId(),
                e.getAnnotation(),
                catDto,
                confirmedRequests,
                e.getEventDate() != null ? e.getEventDate().format(FORMATTER) : null,
                userDto,
                e.getPaid(),
                e.getTitle(),
                views,
                rating
        );
    }

    public static EventShortDto toShort(Event e, CategoryDto catDto, UserShortDto userDto,
                                        long views, long confirmedRequests) {
        return toShort(e, catDto, userDto, views, confirmedRequests, null);
    }

    public static EventShortDto toShort(Event e, long views) {
        return new EventShortDto(
                e.getId(),
                e.getAnnotation(),
                null,
                0L,
                e.getEventDate() != null ? e.getEventDate().format(FORMATTER) : null, // ← ИСПРАВЛЕНО!
                null,
                e.getPaid(),
                e.getTitle(),
                views,
                null
        );
    }

    public static EventFullDto toFull(Event e, CategoryDto catDto, UserShortDto userDto,
                                      long views, long confirmedRequests, RatingDto rating) {
        LocationDto loc = new LocationDto(e.getLocationLat(), e.getLocationLon());

        return EventFullDto.builder()
                .id(e.getId())
                .annotation(e.getAnnotation())
                .description(e.getDescription())
                .category(catDto)
                .confirmedRequests(confirmedRequests)
                .eventDate(e.getEventDate() != null ? e.getEventDate().format(FORMATTER) : null)
                .initiator(userDto)
                .location(loc)
                .paid(e.getPaid())
                .participantLimit(e.getParticipantLimit())
                .requestModeration(e.getRequestModeration())
                .state(e.getState())
                .createdOn(e.getCreatedOn() != null ? e.getCreatedOn().format(FORMATTER) : null)
                .publishedOn(e.getPublishedOn() != null ? e.getPublishedOn().format(FORMATTER) : null)
                .title(e.getTitle())
                .views(views)
                .rating(rating)
                .build();
    }
    public static EventFullDto toFull(Event e, CategoryDto catDto, UserShortDto userDto,
                                      long views, long confirmedRequests) {
        return toFull(e, catDto, userDto, views, confirmedRequests, null);
    }

    public static void applyUserUpdate(Event e, UpdateEventUserRequest dto) {
        if (dto.getAnnotation() != null) e.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) e.setDescription(dto.getDescription());
        if (dto.getTitle() != null) e.setTitle(dto.getTitle());
        if (dto.getCategory() != null) e.setCategoryId(dto.getCategory());
        if (dto.getEventDate() != null) e.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) {
            e.setLocationLat(dto.getLocation().getLat());
            e.setLocationLon(dto.getLocation().getLon());
        }
        if (dto.getPaid() != null) e.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) e.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) e.setRequestModeration(dto.getRequestModeration());
    }

    public static void applyAdminUpdate(Event e, UpdateEventAdminRequest dto) {
        if (dto.getAnnotation() != null) e.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) e.setDescription(dto.getDescription());
        if (dto.getTitle() != null) e.setTitle(dto.getTitle());
        if (dto.getCategory() != null) e.setCategoryId(dto.getCategory());
        if (dto.getEventDate() != null) e.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) {
            e.setLocationLat(dto.getLocation().getLat());
            e.setLocationLon(dto.getLocation().getLon());
        }
        if (dto.getPaid() != null) e.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) e.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) e.setRequestModeration(dto.getRequestModeration());
    }
}