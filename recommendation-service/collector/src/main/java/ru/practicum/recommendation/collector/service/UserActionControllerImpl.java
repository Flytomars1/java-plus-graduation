package ru.practicum.recommendation.collector.service;

import lombok.extern.slf4j.Slf4j;
import ru.practicum.ewm.stats.proto.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;

@Slf4j
@GrpcService
public class UserActionControllerImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaProducerService kafkaProducerService;

    public UserActionControllerImpl(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        long timestampMillis = request.getTimestamp().getSeconds() * 1000L
                + request.getTimestamp().getNanos() / 1_000_000;

        log.info("Received user action: userId={}, eventId={}, actionType={}, timestamp={}",
                request.getUserId(), request.getEventId(), request.getActionType(), timestampMillis);

        try {
            UserActionAvro avroMessage = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(convertActionType(request.getActionType()))
                    .setTimestamp(timestampMillis)
                    .build();

            kafkaProducerService.sendUserAction(avroMessage, request.getUserId());

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(e);
        }
    }

    private ActionTypeAvro convertActionType(ActionTypeProto actionType) {
        switch (actionType) {
            case ACTION_VIEW: return ActionTypeAvro.VIEW;
            case ACTION_REGISTER: return ActionTypeAvro.REGISTER;
            case ACTION_LIKE: return ActionTypeAvro.LIKE;
            default:
                log.warn("Unknown action type: {}, defaulting to VIEW", actionType);
                return ActionTypeAvro.VIEW;
        }
    }
}