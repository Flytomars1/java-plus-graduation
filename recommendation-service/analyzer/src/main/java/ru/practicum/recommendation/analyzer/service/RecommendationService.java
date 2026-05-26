package ru.practicum.recommendation.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.practicum.recommendation.analyzer.model.EventSimilarityEntity;
import ru.practicum.recommendation.analyzer.model.UserActionEntity;
import ru.practicum.recommendation.analyzer.repository.EventSimilarityRepository;
import ru.practicum.recommendation.analyzer.repository.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationService {

    @Autowired
    private UserActionRepository userActionRepository;

    @Autowired
    private EventSimilarityRepository eventSimilarityRepository;

    public List<Map.Entry<Long, Double>> getRecommendationsForUser(Long userId, Integer maxResults) {
        List<Long> interactedEventsList = userActionRepository.findEventIdsByUserId(userId);

        if (interactedEventsList.isEmpty()) {
            log.debug("User {} has no interactions", userId);
            return Collections.emptyList();
        }

        List<UserActionEntity> recentActions = userActionRepository.findByUserId(userId);
        List<Long> recentEventsList = recentActions.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .map(UserActionEntity::getEventId)
                .distinct()
                .limit(5)
                .collect(Collectors.toList());

        if (recentEventsList.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> recentEvents = new HashSet<>(recentEventsList);
        Set<Long> interactedEvents = new HashSet<>(interactedEventsList);

        List<EventSimilarityEntity> allSimilarities = eventSimilarityRepository.findAllByEventIds(recentEventsList);

        Map<Long, Double> recommendations = new HashMap<>();

        for (EventSimilarityEntity sim : allSimilarities) {
            long recommendedEventId;

            if (recentEvents.contains(sim.getEventA())) {
                recommendedEventId = sim.getEventB();
            } else if (recentEvents.contains(sim.getEventB())) {
                recommendedEventId = sim.getEventA();
            } else {
                continue;
            }

            if (!interactedEvents.contains(recommendedEventId)) {
                double currentScore = recommendations.getOrDefault(recommendedEventId, 0.0);
                recommendations.put(recommendedEventId, Math.max(currentScore, sim.getScore()));
            }
        }

        return recommendations.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<Long, Double>> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        List<Long> interactedEvents = userActionRepository.findEventIdsByUserId(userId);
        List<EventSimilarityEntity> similarEvents = eventSimilarityRepository.findSimilarEvents(eventId, 0.0);
        Set<Long> interactedSet = new HashSet<>(interactedEvents);

        return similarEvents.stream()
                .map(sim -> {
                    long similarEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return new AbstractMap.SimpleEntry<>(similarEventId, sim.getScore());
                })
                .filter(entry -> !interactedSet.contains(entry.getKey()))
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<Long, Double>> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object[]> results = userActionRepository.sumWeightsByEventIds(eventIds);

        Map<Long, Double> resultMap = results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).doubleValue()
                ));

        for (Long eventId : eventIds) {
            resultMap.putIfAbsent(eventId, 0.0);
        }

        return resultMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toList());
    }
}