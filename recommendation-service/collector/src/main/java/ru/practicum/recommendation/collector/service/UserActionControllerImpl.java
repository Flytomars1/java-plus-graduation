package ru.practicum.recommendation.collector.service;

import ru.practicum.ewm.stats.proto.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@GrpcService
public class UserActionControllerImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private static final Logger log = LoggerFactory.getLogger(UserActionControllerImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Received user action: userId={}, eventId={}, actionType={}, timestamp={}",
                request.getUserId(), request.getEventId(), request.getActionType(), request.getTimestamp());

        try {
            Map<String, Object> jsonMessage = new HashMap<>();
            jsonMessage.put("user", request.getUserId());
            jsonMessage.put("event", request.getEventId());
            jsonMessage.put("type", request.getActionType().toString());
            jsonMessage.put("timestamp", Instant.now().toString());

            String json = objectMapper.writeValueAsString(jsonMessage);

            kafkaTemplate.send("stats.user-actions.v1",
                    String.valueOf(request.getUserId()),
                    json);

            log.debug("Sent to Kafka: {}", json);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(e);
        }
    }
}