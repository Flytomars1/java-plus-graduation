package ru.practicum.recommendation.analyzer.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.recommendation.analyzer.model.UserActionEntity;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;

@Component
public class UserActionMapper {

    public UserActionEntity toEntity(UserActionAvro avro) {
        if (avro == null) {
            return null;
        }

        return UserActionEntity.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .actionType(avro.getActionType().toString())
                .weight(getWeight(avro.getActionType()))
                .timestamp(Instant.ofEpochMilli(avro.getTimestamp()))
                .build();
    }

    private double getWeight(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return 0.4;
            case REGISTER:
                return 1.2;
            case LIKE:
                return 2.0;
            default:
                return 0.4;
        }
    }
}