package ru.practicum.request.mapper;

import ru.practicum.dto.event.ParticipationRequestDto;
import ru.practicum.request.model.ParticipationRequest;

public class RequestMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }

        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().name())
                .build();
    }
}