package ru.practicum.main.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.event.*;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.dto.rating.RatingDto;
import ru.practicum.main.dto.user.UserShortDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.mapper.RequestMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.rating.RatingService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final StatsClient statsClient;
    private final RatingService ratingService;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours in the future");
        }
        Event e = EventMapper.toEntity(dto, userId);
        Event saved = eventRepository.save(e);
        return enrichFullDto(saved);
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

        if (!Objects.equals(e.getInitiatorId(), userId)) {
            throw new NotFoundException("Event not found for this user");
        }
        if (e.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Cannot edit published event");
        }
        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours in the future");
        }

        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == UpdateEventUserRequest.StateAction.CANCEL_REVIEW) {
                e.setState(EventState.CANCELED);
            } else if (dto.getStateAction() == UpdateEventUserRequest.StateAction.SEND_TO_REVIEW) {
                e.setState(EventState.PENDING);
            }
        }

        EventMapper.applyUserUpdate(e, dto);
        return enrichFullDto(eventRepository.save(e));
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!Objects.equals(e.getInitiatorId(), userId)) {
            throw new NotFoundException("Event not found for this user");
        }
        return enrichFullDto(e);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        if (!userRepository.existsById(userId)) throw new NotFoundException("User not found");
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "id"));
        return eventRepository.findAllByInitiatorId(userId, pageable).getContent().stream()
                .map(this::enrichShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not initiator");
        }
        return requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not initiator");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByEventIdAndIdIn(eventId, dto.getRequestIds());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        for (ParticipationRequest req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must be PENDING");
            }
            if (dto.getStatus() == EventRequestStatusUpdateRequest.Status.CONFIRMED) {
                if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejected.add(RequestMapper.toDto(requestRepository.save(req)));
                } else {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(requestRepository.save(req)));
                    confirmedCount++;
                }
            } else {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toDto(requestRepository.save(req)));
            }
        }
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            Boolean onlyAvailable, String sort, int from, int size,
                                            String requestUri, String ip) {

        safeAddHit(requestUri, ip);

        if (rangeStart == null && rangeEnd == null) rangeStart = LocalDateTime.now();
        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) throw new IllegalArgumentException("End before start");

        Specification<Event> spec = Specification.where(published())
                .and(betweenDates(rangeStart, rangeEnd))
                .and(categories == null || categories.isEmpty() ? null : inCategories(categories))
                .and(paid == null ? null : paidEq(paid))
                .and(text == null || text.isBlank() ? null : textLike(text));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("eventDate"));
        Page<Event> page = eventRepository.findAll(spec, pageable);

        Comparator<EventShortDto> comparator;
        if ("VIEWS".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(EventShortDto::getViews).reversed();
        } else if ("RATING_DESC".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(dto -> dto.getRating() != null ? dto.getRating().getScore() : 0,
                    Comparator.reverseOrder());
        } else if ("RATING_ASC".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(dto -> dto.getRating() != null ? dto.getRating().getScore() : 0);
        } else {
            comparator = Comparator.comparing(EventShortDto::getEventDate);
        }

        return page.getContent().stream()
                .map(this::enrichShortDto)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicById(Long eventId, String requestUri, String ip) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (e.getState() != EventState.PUBLISHED) throw new NotFoundException("Event must be published");

        safeAddHit("/events/" + eventId, ip);

        long views = fetchViews(eventId);

        if (views == 0) {
            views = 1;
        }

        return enrichFullDtoWithViews(e, views);
    }

    @Override
    public List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Specification<Event> spec = Specification.where(betweenDates(rangeStart, rangeEnd))
                .and(users == null || users.isEmpty() ? null : initiatorsIn(users))
                .and(states == null || states.isEmpty() ? null : stateIn(states))
                .and(categories == null || categories.isEmpty() ? null : inCategories(categories));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        return eventRepository.findAll(spec, pageable).getContent().stream()
                .map(this::enrichFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Date too early");
        }

        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == UpdateEventAdminRequest.StateAction.PUBLISH_EVENT) {
                if (e.getState() != EventState.PENDING) throw new ConflictException("Cannot publish not pending");
                e.setState(EventState.PUBLISHED);
                e.setPublishedOn(LocalDateTime.now());
            } else if (dto.getStateAction() == UpdateEventAdminRequest.StateAction.REJECT_EVENT) {
                if (e.getState() == EventState.PUBLISHED) throw new ConflictException("Cannot reject published");
                e.setState(EventState.CANCELED);
            }
        }
        EventMapper.applyAdminUpdate(e, dto);
        return enrichFullDto(eventRepository.save(e));
    }

    /* Helpers */
    private void safeAddHit(String uri, String ip) {
        try {
            statsClient.hit(buildHit("ewm-main-service", uri, ip, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Could not save stats hit: {}", e.getMessage());
        }
    }

    private EventFullDto enrichFullDto(Event e) {
        long views = fetchViews(e.getId());
        return enrichFullDtoWithViews(e, views);
    }

    private EventFullDto enrichFullDtoWithViews(Event e, long views) {
        long confirmed = requestRepository.countByEventIdAndStatus(e.getId(), RequestStatus.CONFIRMED);
        Category cat = categoryRepository.findById(e.getCategoryId()).orElse(null);
        CategoryDto catDto = cat != null ? new CategoryDto(cat.getId(), cat.getName()) : null;
        User user = userRepository.findById(e.getInitiatorId()).orElse(null);
        UserShortDto userDto = user != null ? new UserShortDto(user.getId(), user.getName()) : null;

        RatingDto rating = ratingService.getEventRating(e.getId());

        return EventMapper.toFull(e, catDto, userDto, views, confirmed, rating);
    }

    private EventShortDto enrichShortDto(Event e) {
        long views = fetchViews(e.getId());
        long confirmed = requestRepository.countByEventIdAndStatus(e.getId(), RequestStatus.CONFIRMED);
        Category cat = categoryRepository.findById(e.getCategoryId()).orElse(null);
        CategoryDto catDto = cat != null ? new CategoryDto(cat.getId(), cat.getName()) : null;
        User user = userRepository.findById(e.getInitiatorId()).orElse(null);
        UserShortDto userDto = user != null ? new UserShortDto(user.getId(), user.getName()) : null;

        RatingDto rating = ratingService.getEventRating(e.getId());

        return EventMapper.toShort(e, catDto, userDto, views, confirmed, rating);
    }

    private long fetchViews(Long eventId) {
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now().plusSeconds(1),
                    List.of("/events/" + eventId),
                    true
            );
            return stats.isEmpty() ? 0 : stats.get(0).getHits();
        } catch (Exception e) {
            return 0;
        }
    }

    private HitDto buildHit(String app, String uri, String ip, LocalDateTime ts) {
        HitDto hit = new HitDto();
        hit.setApp(app);
        hit.setUri(uri);
        hit.setIp(ip);
        hit.setTimestamp(ts);
        return hit;
    }

    private Specification<Event> published() { return (r, q, cb) -> cb.equal(r.get("state"), EventState.PUBLISHED); }
    private Specification<Event> betweenDates(LocalDateTime start, LocalDateTime end) {
        return (r, q, cb) -> {
            if (start == null && end == null) return cb.conjunction();
            if (end == null) return cb.greaterThanOrEqualTo(r.get("eventDate"), start);
            if (start == null) return cb.lessThanOrEqualTo(r.get("eventDate"), end);
            return cb.between(r.get("eventDate"), start, end);
        };
    }
    private Specification<Event> inCategories(List<Long> cats) { return (r, q, cb) -> r.get("categoryId").in(cats); }
    private Specification<Event> paidEq(Boolean paid) { return (r, q, cb) -> cb.equal(r.get("paid"), paid); }
    private Specification<Event> textLike(String text) {
        String p = "%" + text.toLowerCase() + "%";
        return (r, q, cb) -> cb.or(cb.like(cb.lower(r.get("annotation")), p), cb.like(cb.lower(r.get("description")), p));
    }
    private Specification<Event> initiatorsIn(List<Long> users) { return (r, q, cb) -> r.get("initiatorId").in(users); }
    private Specification<Event> stateIn(List<String> states) {
        return (r, q, cb) -> r.get("state").in(states.stream().map(s -> EventState.valueOf(s.toUpperCase())).collect(Collectors.toList()));
    }
}