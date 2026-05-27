package ru.practicum.event.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.rating.RatingDto;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.event.service.event.recommendation.RecommendationGrpcService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.event.mapper.EventMapper;
import ru.practicum.event.model.Category;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.client.RatingClient;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.HitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private final RatingClient ratingClient;
    private final UserClient userClient;
    private final RequestClient requestClient;
    private final EventCircuitBreakerService circuitBreakerService;
    private final EventUpdater eventUpdater;
    private final RecommendationGrpcService recommendationGrpcService;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        if (!Boolean.TRUE.equals(userClient.userExists(userId))) {
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
            if (dto.getStateAction() == UserStateAction.CANCEL_REVIEW) {
                e.setState(EventState.CANCELED);
            } else if (dto.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
                e.setState(EventState.PENDING);
            }
        }

        eventUpdater.applyUserUpdate(e, dto);
        return enrichFullDto(eventRepository.save(e));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest dto) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not initiator");
        }

        Long confirmedCount = requestClient.getConfirmedRequestsCount(eventId);

        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }

        return requestClient.updateRequestStatus(eventId, dto);
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));

        if (dto.getEventDate() != null && dto.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new ConflictException("Date too early");
        }

        if (dto.getStateAction() != null) {
            if (dto.getStateAction() == AdminStateAction.PUBLISH_EVENT) {
                if (e.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish not pending");
                }
                e.setState(EventState.PUBLISHED);
                e.setPublishedOn(LocalDateTime.now());
            } else if (dto.getStateAction() == AdminStateAction.REJECT_EVENT) {
                if (e.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject published");
                }
                e.setState(EventState.CANCELED);
            }
        }
        eventUpdater.applyAdminUpdate(e, dto);
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
    public List<EventShortDto> getUserEvents(Long userId, Integer from, Integer size) {
        if (!Boolean.TRUE.equals(userClient.userExists(userId))) {
            throw new NotFoundException("User not found");
        }
        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "id"));
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable).getContent();
        return enrichShortDtos(events);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException("User is not initiator");
        }
        return requestClient.getRequestsByEventId(eventId);
    }

    @Override
    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            Boolean onlyAvailable, String sort, Integer from, Integer size,
                                            String requestUri, String ip) {

        safeAddHit(requestUri, ip);

        if (rangeStart == null && rangeEnd == null) rangeStart = LocalDateTime.now();
        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new IllegalArgumentException("End before start");
        }

        Specification<Event> spec = Specification.where(published())
                .and(betweenDates(rangeStart, rangeEnd))
                .and(categories == null || categories.isEmpty() ? null : inCategories(categories))
                .and(paid == null ? null : paidEq(paid))
                .and(text == null || text.isBlank() ? null : textLike(text));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("eventDate"));
        Page<Event> page = eventRepository.findAll(spec, pageable);

        Comparator<EventShortDto> comparator;
        if ("VIEWS".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(EventShortDto::getViews,
                    Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("RATING_DESC".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(dto -> dto.getRating() != null ? dto.getRating().getScore() : 0,
                    Comparator.reverseOrder());
        } else if ("RATING_ASC".equalsIgnoreCase(sort)) {
            comparator = Comparator.comparing(dto -> dto.getRating() != null ? dto.getRating().getScore() : 0);
        } else {
            comparator = Comparator.comparing(EventShortDto::getEventDate, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        List<EventShortDto> dtos = enrichShortDtos(page.getContent());
        dtos.sort(comparator);
        return dtos;
    }

    @Override
    public EventFullDto getPublicById(Long eventId, String requestUri, String ip, Long userId) {
        Event e = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (e.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event must be published");
        }

        if (userId != null) {
            recommendationGrpcService.sendView(userId, eventId);
        }

        safeAddHit("/events/" + eventId, ip);

        long views = fetchViews(eventId);
        if (views == 0) {
            views = 1;
        }

        return enrichFullDto(e, views);
    }

    @Override
    public List<EventFullDto> searchAdmin(List<Long> users, List<String> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        Specification<Event> spec = Specification.where(betweenDates(rangeStart, rangeEnd))
                .and(users == null || users.isEmpty() ? null : initiatorsIn(users))
                .and(states == null || states.isEmpty() ? null : stateIn(states))
                .and(categories == null || categories.isEmpty() ? null : inCategories(categories));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id"));
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        return enrichFullDtos(events);
    }

    @Override
    public boolean existsById(Long eventId) {
        log.debug("Checking if event exists by id: {}", eventId);
        return eventRepository.existsById(eventId);
    }

    @Override
    public EventShortDto getEventShortById(Long eventId) {
        log.debug("Getting event short by id: {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found with id: " + eventId));

        EventShortDto dto = EventMapper.toShort(event, 0L);
        dto.setState(event.getState().name());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());

        if (event.getInitiatorId() != null) {
            UserShortDto initiator = circuitBreakerService.getUserShortById(event.getInitiatorId());
            dto.setInitiator(initiator);
        }

        return dto;
    }

    @Override
    public boolean isEventPublished(Long eventId) {
        log.debug("Checking if event is published: {}", eventId);
        return eventRepository.findById(eventId)
                .map(event -> event.getState() == EventState.PUBLISHED)
                .orElse(false);
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, Integer from, Integer size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        List<Long> recommendedEventIds = recommendationGrpcService.getRecommendationsForUser(userId, from + size);

        if (recommendedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> paginatedIds = recommendedEventIds.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());

        List<Event> events = eventRepository.findAllById(paginatedIds);

        Map<Long, Event> eventMap = events.stream()
                .collect(Collectors.toMap(Event::getId, Function.identity()));

        List<Event> orderedEvents = paginatedIds.stream()
                .map(eventMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return enrichShortDtos(orderedEvents);
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot like unpublished event");
        }

        boolean hasRequest = requestClient.hasRequest(userId, eventId);
        if (!hasRequest) {
            throw new IllegalArgumentException("User can only like events they have visited");
        }

        recommendationGrpcService.sendLike(userId, eventId);
    }

    private EventDataBundle prepareEventDataBundle(List<Event> events) {
        if (events.isEmpty()) {
            return new EventDataBundle(Collections.emptyMap(), Collections.emptyMap(),
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        }

        Set<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .collect(Collectors.toSet());
        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        Map<Long, Category> categoriesMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
        Map<Long, CategoryDto> categoryDtoMap = categoriesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new CategoryDto(e.getValue().getId(), e.getValue().getName())));

        Map<Long, Long> viewsMap = fetchViewsBatch(eventIds);
        Map<Long, Long> confirmedMap = fetchConfirmedCountsBatch(eventIds);
        Map<Long, UserShortDto> usersMap = fetchUsersBatch(initiatorIds);
        Map<Long, RatingDto> ratingsMap = fetchRatingsBatch(eventIds);

        return new EventDataBundle(categoryDtoMap, viewsMap, confirmedMap, usersMap, ratingsMap);
    }

    private record EventDataBundle(
            Map<Long, CategoryDto> categoryDtoMap,
            Map<Long, Long> viewsMap,
            Map<Long, Long> confirmedMap,
            Map<Long, UserShortDto> usersMap,
            Map<Long, RatingDto> ratingsMap
    ) {}

    private List<EventFullDto> enrichFullDtos(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        EventDataBundle bundle = prepareEventDataBundle(events);

        return events.stream()
                .map(event -> {
                    CategoryDto catDto = bundle.categoryDtoMap.get(event.getCategoryId());
                    UserShortDto userDto = bundle.usersMap.get(event.getInitiatorId());
                    Long views = bundle.viewsMap.getOrDefault(event.getId(), 0L);
                    Long confirmed = bundle.confirmedMap.getOrDefault(event.getId(), 0L);
                    RatingDto rating = bundle.ratingsMap.get(event.getId());
                    return EventMapper.toFull(event, catDto, userDto, views, confirmed, rating);
                })
                .collect(Collectors.toList());
    }

    private List<EventShortDto> enrichShortDtos(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        EventDataBundle bundle = prepareEventDataBundle(events);

        return events.stream()
                .map(event -> {
                    CategoryDto catDto = bundle.categoryDtoMap.get(event.getCategoryId());
                    UserShortDto userDto = bundle.usersMap.get(event.getInitiatorId());
                    Long views = bundle.viewsMap.getOrDefault(event.getId(), 0L);
                    Long confirmed = bundle.confirmedMap.getOrDefault(event.getId(), 0L);
                    RatingDto rating = bundle.ratingsMap.get(event.getId());
                    return EventMapper.toShort(event, catDto, userDto, views, confirmed, rating);
                })
                .collect(Collectors.toList());
    }

    private EventFullDto enrichFullDto(Event e) {
        long views = fetchViews(e.getId());
        return enrichFullDto(e, views);
    }

    private EventFullDto enrichFullDto(Event e, long views) {
        Category cat = categoryRepository.findById(e.getCategoryId()).orElse(null);
        CategoryDto catDto = cat != null ? new CategoryDto(cat.getId(), cat.getName()) : null;

        UserShortDto userDto = circuitBreakerService.getUserShortById(e.getInitiatorId());
        Long confirmed = circuitBreakerService.getConfirmedRequestsCount(e.getId());
        RatingDto rating = circuitBreakerService.getEventRating(e.getId());

        return EventMapper.toFull(e, catDto, userDto, views, confirmed, rating);
    }

    private Map<Long, Long> fetchViewsBatch(List<Long> eventIds) {
        Map<Long, Long> result = new HashMap<>();
        for (Long eventId : eventIds) {
            result.put(eventId, fetchViews(eventId));
        }
        return result;
    }

    private Map<Long, Long> fetchConfirmedCountsBatch(List<Long> eventIds) {
        try {
            return requestClient.getConfirmedRequestsCounts(eventIds);
        } catch (Exception e) {
            log.warn("Failed to fetch confirmed counts batch: {}", e.getMessage());
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0L));
        }
    }

    private Map<Long, UserShortDto> fetchUsersBatch(Set<Long> userIds) {
        try {
            List<UserShortDto> users = userClient.getUsersShortByIds(new ArrayList<>(userIds));
            return users.stream()
                    .collect(Collectors.toMap(UserShortDto::getId, user -> user));
        } catch (Exception e) {
            log.warn("Failed to fetch users batch: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<Long, RatingDto> fetchRatingsBatch(List<Long> eventIds) {
        try {
            return ratingClient.getEventRatings(eventIds);
        } catch (Exception e) {
            log.warn("Failed to fetch ratings batch: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void safeAddHit(String uri, String ip) {
        try {
            statsClient.hit(buildHit("ewm-event-service", uri, ip, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Could not save stats hit: {}", e.getMessage());
        }
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

    // Specification methods
    private Specification<Event> published() {
        return (r, q, cb) -> cb.equal(r.get("state"), EventState.PUBLISHED);
    }

    private Specification<Event> betweenDates(LocalDateTime start, LocalDateTime end) {
        return (r, q, cb) -> {
            if (start == null && end == null) return cb.conjunction();
            if (end == null) return cb.greaterThanOrEqualTo(r.get("eventDate"), start);
            if (start == null) return cb.lessThanOrEqualTo(r.get("eventDate"), end);
            return cb.between(r.get("eventDate"), start, end);
        };
    }

    private Specification<Event> inCategories(List<Long> cats) {
        return (r, q, cb) -> r.get("categoryId").in(cats);
    }

    private Specification<Event> paidEq(Boolean paid) {
        return (r, q, cb) -> cb.equal(r.get("paid"), paid);
    }

    private Specification<Event> textLike(String text) {
        String p = "%" + text.toLowerCase() + "%";
        return (r, q, cb) -> cb.or(
                cb.like(cb.lower(r.get("annotation")), p),
                cb.like(cb.lower(r.get("description")), p));
    }

    private Specification<Event> initiatorsIn(List<Long> users) {
        return (r, q, cb) -> r.get("initiatorId").in(users);
    }

    private Specification<Event> stateIn(List<String> states) {
        return (r, q, cb) -> r.get("state").in(
                states.stream().map(EventState::valueOf).collect(Collectors.toList()));
    }
}