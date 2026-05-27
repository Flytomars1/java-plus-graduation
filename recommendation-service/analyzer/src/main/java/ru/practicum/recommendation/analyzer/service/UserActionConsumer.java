package ru.practicum.recommendation.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.recommendation.analyzer.mapper.UserActionMapper;
import ru.practicum.recommendation.analyzer.model.UserActionEntity;
import ru.practicum.recommendation.analyzer.repository.UserActionRepository;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.avro.UserActionAvroSerializer;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private final UserActionRepository userActionRepository;
    private final UserActionMapper userActionMapper;

    @KafkaListener(topics = "stats.user-actions.v1", groupId = "analyzer-group")
    public void consume(byte[] message) {
        try {
            UserActionAvro userActionAvro = UserActionAvroSerializer.deserialize(message);
            log.debug("Received user action: userId={}, eventId={}, actionType={}",
                    userActionAvro.getUserId(), userActionAvro.getEventId(), userActionAvro.getActionType());

            UserActionEntity entity = userActionMapper.toEntity(userActionAvro);
            userActionRepository.save(entity);
            log.debug("Saved user action to database");

        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }
}