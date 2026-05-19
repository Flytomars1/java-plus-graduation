package ru.practicum.dto.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingDto {
    private Long id;
    private Long userId;
    private Long eventId;
    private Boolean isLike;
    private LocalDateTime created;
}