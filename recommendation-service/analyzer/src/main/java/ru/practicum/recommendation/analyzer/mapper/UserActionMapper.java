package ru.practicum.recommendation.analyzer.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.recommendation.analyzer.model.UserActionEntity;
import ru.practicum.recommendation.avro.ActionTypeAvro;
import ru.practicum.recommendation.avro.UserActionAvro;

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
                .timestamp(avro.getTimestamp())
                .build();
    }

    private double getWeight(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return 1.0;
            case REGISTER:
                return 3.0;
            case LIKE:
                return 5.0;
            default:
                return 1.0;
        }
    }
}