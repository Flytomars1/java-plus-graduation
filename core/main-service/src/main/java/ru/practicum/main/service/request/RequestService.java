package ru.practicum.main.service.request;

import ru.practicum.main.dto.event.ParticipationRequestDto;
import java.util.List;

public interface RequestService {
    List<ParticipationRequestDto> getUserRequests(Long userId);
    ParticipationRequestDto addRequest(Long userId, Long eventId);
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}