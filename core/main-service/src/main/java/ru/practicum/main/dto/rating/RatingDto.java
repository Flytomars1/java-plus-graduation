package ru.practicum.main.dto.rating;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class RatingDto {
    private Integer score;
    private Long likes;
    private Long dislikes;
    private Long total;
}