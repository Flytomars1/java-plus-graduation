package ru.practicum.recommendation.aggregator.service;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimilarityCalculator {

    private static final Logger log = LoggerFactory.getLogger(SimilarityCalculator.class);
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();
    private final Map<Long, Double> eventTotalSums = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

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

        try {
            byte[] data = serialize(similarity);
            kafkaTemplate.send("stats.events-similarity.v1",
                    String.valueOf(first),
                    data);
            log.debug("Sent similarity: eventA={}, eventB={}, score={}", first, second, score);
        } catch (Exception e) {
            log.error("Error sending similarity", e);
        }
    }

    private double getWeightByActionType(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW: return 0.4;
            case REGISTER: return 0.8;
            case LIKE: return 1.0;
            default: return 0.4;
        }
    }

    private byte[] serialize(EventSimilarityAvro data) throws IOException {
        SpecificDatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.getClassSchema());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(data, encoder);
        encoder.flush();
        return out.toByteArray();
    }
}