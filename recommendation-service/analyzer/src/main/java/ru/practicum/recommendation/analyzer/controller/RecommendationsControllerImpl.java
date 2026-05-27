package ru.practicum.recommendation.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.recommendation.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;
import java.util.Map;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequest request,
                                          StreamObserver<RecommendedEvent> responseObserver) {
        log.info("Getting recommendations for user: {}", request.getUserId());

        try {
            List<Map.Entry<Long, Double>> recommendations =
                    recommendationService.getRecommendationsForUser(
                            request.getUserId(),
                            request.getMaxResults()
                    );

            for (Map.Entry<Long, Double> rec : recommendations) {
                RecommendedEvent event = RecommendedEvent.newBuilder()
                        .setEventId(rec.getKey())
                        .setScore(rec.getValue())
                        .build();
                responseObserver.onNext(event);
            }

            responseObserver.onCompleted();
            log.info("Sent {} recommendations for user {}", recommendations.size(), request.getUserId());

        } catch (Exception e) {
            log.error("Error getting recommendations for user {}", request.getUserId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequest request,
                                 StreamObserver<RecommendedEvent> responseObserver) {
        log.info("Getting similar events for event: {}, user: {}", request.getEventId(), request.getUserId());

        try {
            List<Map.Entry<Long, Double>> similarEvents =
                    recommendationService.getSimilarEvents(
                            request.getEventId(),
                            request.getUserId(),
                            request.getMaxResults()
                    );

            for (Map.Entry<Long, Double> sim : similarEvents) {
                RecommendedEvent event = RecommendedEvent.newBuilder()
                        .setEventId(sim.getKey())
                        .setScore(sim.getValue())
                        .build();
                responseObserver.onNext(event);
            }

            responseObserver.onCompleted();
            log.info("Sent {} similar events", similarEvents.size());

        } catch (Exception e) {
            log.error("Error getting similar events", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequest request,
                                     StreamObserver<RecommendedEvent> responseObserver) {
        log.info("Getting interactions count for events: {}", request.getEventIdsList());

        try {
            List<Map.Entry<Long, Double>> counts =
                    recommendationService.getInteractionsCount(request.getEventIdsList());

            for (Map.Entry<Long, Double> count : counts) {
                RecommendedEvent event = RecommendedEvent.newBuilder()
                        .setEventId(count.getKey())
                        .setScore(count.getValue())
                        .build();
                responseObserver.onNext(event);
            }

            responseObserver.onCompleted();
            log.info("Sent {} interaction counts", counts.size());

        } catch (Exception e) {
            log.error("Error getting interactions count", e);
            responseObserver.onError(e);
        }
    }
}