package ru.practicum.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.dto.user.UserShortDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private String eventDate;
    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
    private RatingDto rating;
    private String state;
    private Integer participantLimit;
    private Boolean requestModeration;

    public EventShortDto(Long id, String annotation, CategoryDto category, Long confirmedRequests,
                         String eventDate, UserShortDto initiator, Boolean paid, String title, Long views) {
        this(id, annotation, category, confirmedRequests, eventDate, initiator, paid, title, views, null, null, null, null);
    }
}