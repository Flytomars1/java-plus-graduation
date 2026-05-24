package ru.practicum.recommendation.collector.service;

import ru.practicum.recommendation.proto.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.recommendation.avro.ActionTypeAvro;
import ru.practicum.recommendation.avro.UserActionAvro;
import ru.practicum.recommendation.proto.ActionType;
import ru.practicum.recommendation.proto.UserAction;
import ru.practicum.recommendation.proto.UserActionServiceGrpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

@GrpcService
public class UserActionControllerImpl extends UserActionServiceGrpc.UserActionServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(UserActionControllerImpl.class);

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Override
    public void collectUserAction(UserAction request, StreamObserver<Empty> responseObserver) {
        log.info("Received user action: userId={}, eventId={}, actionType={}, timestamp={}",
                request.getUserId(), request.getEventId(), request.getActionType(), request.getTimestamp());

        try {
            UserActionAvro avroMessage = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(convertActionType(request.getActionType()))
                    .setTimestamp(Instant.ofEpochMilli(request.getTimestamp()))
                    .build();

            byte[] data = serialize(avroMessage);

            kafkaTemplate.send("stats.user-actions.v1",
                    String.valueOf(request.getUserId()),
                    data);

            log.debug("Sent to Kafka: {}", avroMessage);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(e);
        }
    }

    private byte[] serialize(UserActionAvro data) throws IOException {
        SpecificDatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.getClassSchema());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(data, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    private ActionTypeAvro convertActionType(ActionType actionType) {
        switch (actionType) {
            case ACTION_VIEW:
                return ActionTypeAvro.VIEW;
            case ACTION_REGISTER:
                return ActionTypeAvro.REGISTER;
            case ACTION_LIKE:
                return ActionTypeAvro.LIKE;
            default:
                log.warn("Unknown action type: {}, defaulting to VIEW", actionType);
                return ActionTypeAvro.VIEW;
        }
    }
}