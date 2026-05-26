package ru.practicum.recommendation.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SimilarityCalculator {

    private final Map<Long, Map<Long, Double>> userEventWeights;
    private final Map<Long, Double> eventTotalSums;
    private final Map<Long, Map<Long, Double>> minWeightsSums;
    private final KafkaProducerService kafkaProducerService;

    public SimilarityCalculator(KafkaProducerService kafkaProducerService) {
        this.userEventWeights = new ConcurrentHashMap<>();
        this.eventTotalSums = new ConcurrentHashMap<>();
        this.minWeightsSums = new ConcurrentHashMap<>();
        this.kafkaProducerService = kafkaProducerService;
    }

    public void processUserAction(UserActionAvro action) {
        long eventId = action.getEventId();
        long userId = action.getUserId();
        double weight = getWeightByActionType(action.getActionType());

        Double previousMaxWeight = userEventWeights
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>())
                .get(userId);

        if (previousMaxWeight != null && previousMaxWeight >= weight) {
            log.debug("Weight not changed for user {} event {}, skipping", userId, eventId);
            return;
        }

        userEventWeights.get(eventId).put(userId, weight);

        updateSimilarities(eventId, userId, previousMaxWeight, weight);
    }

    private void updateSimilarities(long eventA, long userId, Double oldWeight, double newWeight) {
        double delta = (oldWeight == null ? newWeight : newWeight - oldWeight);

        for (Map.Entry<Long, Map<Long, Double>> entry : userEventWeights.entrySet()) {
            long eventB = entry.getKey();
            if (eventB == eventA) continue;

            Double weightB = entry.getValue().get(userId);
            if (weightB == null) continue;

            double newMin = Math.min(newWeight, weightB);
            double oldMin = (oldWeight != null) ? Math.min(oldWeight, weightB) : 0.0;
            updateMinWeightSum(eventA, eventB, newMin, oldMin);
        }

        eventTotalSums.merge(eventA, delta, Double::sum);

        for (Map.Entry<Long, Map<Long, Double>> entry : userEventWeights.entrySet()) {
            long eventB = entry.getKey();
            if (eventB == eventA) continue;
            if (entry.getValue().get(userId) == null) continue;

            double newScore = calculateSimilarity(eventA, eventB);
            sendSimilarity(eventA, eventB, newScore);
        }
    }

    private void updateMinWeightSum(long eventA, long eventB, double newMin, double oldMin) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        Map<Long, Double> innerMap = minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>());
        Double current = innerMap.getOrDefault(second, 0.0);
        innerMap.put(second, current - oldMin + newMin);
    }

    private double calculateSimilarity(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        Double sumA = eventTotalSums.getOrDefault(eventA, 0.0);
        Double sumB = eventTotalSums.getOrDefault(eventB, 0.0);

        Map<Long, Double> innerMap = minWeightsSums.get(first);
        Double sMin = (innerMap != null) ? innerMap.get(second) : 0.0;

        if (sumA == 0 || sumB == 0) return 0.0;

        double rawScore = sMin / (Math.sqrt(sumA) * Math.sqrt(sumB));
        return rawScore;
    }

    private void sendSimilarity(long eventA, long eventB, double score) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                .setEventA(first)
                .setEventB(second)
                .setScore(score)
                .setTimestamp(Instant.now().toEpochMilli())
                .build();

        kafkaProducerService.sendSimilarity(similarity);
    }

    private double getWeightByActionType(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW: return 0.4;
            case REGISTER: return 0.8;
            case LIKE: return 1.0;
            default: return 0.4;
        }
    }
}