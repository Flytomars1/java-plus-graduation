package ru.practicum.event.service.event.recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.recommendation.client.RecommendationClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEvent;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationGrpcService {

    private final RecommendationClient recommendationClient;

    public void sendView(Long userId, Long eventId) {
        try {
            recommendationClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_VIEW, System.currentTimeMillis());
            log.debug("Sent VIEW action: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.warn("Failed to send VIEW action: {}", e.getMessage());
        }
    }

    public void sendLike(Long userId, Long eventId) {
        try {
            recommendationClient.sendUserAction(userId, eventId, ActionTypeProto.ACTION_LIKE, System.currentTimeMillis());
            log.debug("Sent LIKE action: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.warn("Failed to send LIKE action: {}", e.getMessage());
        }
    }

    public List<Long> getRecommendationsForUser(Long userId, Integer maxResults) {
        try {
            return recommendationClient.getRecommendationsForUser(userId, maxResults)
                    .map(RecommendedEvent::getEventId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get recommendations for user {}: {}", userId, e.getMessage());
            return List.of();
        }
    }
}