package ru.practicum.dto.rating;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RateEventRequest {
    @NotNull
    private Long eventId;

    @NotNull
    private Boolean isLike;
}