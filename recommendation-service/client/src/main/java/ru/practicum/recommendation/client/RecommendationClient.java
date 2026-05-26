package ru.practicum.recommendation.client;

import com.google.protobuf.Internal;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import com.google.protobuf.Timestamp;

@Component
public class RecommendationClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub recommendationsStub;

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void sendUserAction(Long userId, Long eventId, ActionTypeProto actionType, Long timestampMillis) {
        Timestamp timestamp = Timestamp.newBuilder()
                .setSeconds(timestampMillis / 1000)
                .setNanos((int)((timestampMillis % 1000) * 1_000_000))
                .build();

        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(timestamp)
                .build();

        collectorStub.collectUserAction(request);
    }

    public Stream<RecommendedEvent> getRecommendationsForUser(Long userId, Integer maxResults) {
        UserPredictionsRequest request = UserPredictionsRequest.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEvent> iterator = recommendationsStub.getRecommendationsForUser(request);
        return toStream(iterator);
    }

    public Stream<RecommendedEvent> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        SimilarEventsRequest request = SimilarEventsRequest.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEvent> iterator = recommendationsStub.getSimilarEvents(request);
        return toStream(iterator);
    }

    public Stream<RecommendedEvent> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequest.Builder builder = InteractionsCountRequest.newBuilder();
        builder.addAllEventIds(eventIds);

        Iterator<RecommendedEvent> iterator = recommendationsStub.getInteractionsCount(builder.build());
        return toStream(iterator);
    }

    private Stream<RecommendedEvent> toStream(Iterator<RecommendedEvent> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}