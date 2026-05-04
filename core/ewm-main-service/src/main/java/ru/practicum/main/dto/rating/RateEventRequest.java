package ru.practicum.main.dto.rating;

import lombok.*;
import jakarta.validation.constraints.NotNull;

@Data
public class RateEventRequest {
    @NotNull
    private Long eventId;

    @NotNull
    private Boolean isLike;
}