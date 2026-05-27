package ru.practicum.recommendation.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.UserActionAvroSerializer;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final SimilarityCalculator similarityCalculator;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "aggregator-group")
    public void consume(byte[] message) {
        try {
            UserActionAvro userAction = UserActionAvroSerializer.deserialize(message);
            log.debug("Received user action: userId={}, eventId={}, actionType={}",
                    userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

            similarityCalculator.processUserAction(userAction);

        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }
}