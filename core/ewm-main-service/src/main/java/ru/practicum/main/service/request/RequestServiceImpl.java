package ru.practicum.main.service.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.event.ParticipationRequestDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.RequestMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        if (!userRepository.existsById(userId)) throw new NotFoundException("User not found");
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Repeated request");
        }
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Initiator cannot participate");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Event not published");
        }
        if (event.getParticipantLimit() > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= event.getParticipantLimit()) {
                throw new ConflictException("Limit reached");
            }
        }

        RequestStatus status = RequestStatus.PENDING;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        }

        ParticipationRequest req = new ParticipationRequest();
        req.setCreated(LocalDateTime.now());
        req.setRequester(user);
        req.setEvent(event);
        req.setStatus(status);

        return RequestMapper.toDto(requestRepository.save(req));
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest req = requestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Request not found"));
        if (!req.getRequester().getId().equals(userId)) {
            throw new NotFoundException("Request doesn't belong to user");
        }
        req.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(requestRepository.save(req));
    }
}