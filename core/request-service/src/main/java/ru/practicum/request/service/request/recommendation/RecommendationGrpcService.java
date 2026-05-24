package ru.practicum.request.service.request.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.recommendation.client.RecommendationClient;
import ru.practicum.recommendation.proto.ActionType;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationGrpcService {

    private final RecommendationClient recommendationClient;

    public void sendRegister(long userId, long eventId) {
        try {
            recommendationClient.sendUserAction(userId, eventId, ActionType.ACTION_REGISTER, System.currentTimeMillis());
            log.debug("Sent REGISTER action: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.warn("Failed to send REGISTER action: {}", e.getMessage());
        }
    }
}