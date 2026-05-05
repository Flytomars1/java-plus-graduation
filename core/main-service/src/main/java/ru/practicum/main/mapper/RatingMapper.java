package ru.practicum.main.mapper;

import ru.practicum.main.dto.rating.RatingDto;

public class RatingMapper {
    public static RatingDto toRatingDto(Long eventId, Long likes, Long dislikes) {
        long total = likes + dislikes;
        int score = total > 0 ? (int) Math.round((double) likes / total * 100) : 0;

        return RatingDto.builder()
                .score(score)
                .likes(likes)
                .dislikes(dislikes)
                .total(total)
                .build();
    }
}