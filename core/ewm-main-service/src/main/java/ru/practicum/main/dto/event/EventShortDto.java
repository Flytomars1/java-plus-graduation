package ru.practicum.main.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.dto.rating.RatingDto;
import ru.practicum.main.dto.user.UserShortDto;

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

    public EventShortDto(Long id, String annotation, CategoryDto category, Long confirmedRequests,
                         String eventDate, UserShortDto initiator, Boolean paid, String title, Long views) {
        this(id, annotation, category, confirmedRequests, eventDate, initiator, paid, title, views, null);
    }
}