package ru.practicum.ewm.stats.avro;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class UserActionAvroSerializer {

    private UserActionAvroSerializer() {
    }

    public static byte[] serialize(UserActionAvro data) throws IOException {
        SpecificDatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.getClassSchema());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(data, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    public static UserActionAvro deserialize(byte[] data) throws IOException {
        SpecificDatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.getClassSchema());
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }
}