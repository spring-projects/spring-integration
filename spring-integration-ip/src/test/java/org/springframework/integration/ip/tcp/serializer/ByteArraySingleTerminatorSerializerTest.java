package org.springframework.integration.ip.tcp.serializer;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ByteArraySingleTerminatorSerializerTest {

    @Test
    public void canDeserializeMultipleSubsequentTerminators() throws IOException {
        byte terminator = (byte) '\n';
        ByteArraySingleTerminatorSerializer serializer = new ByteArraySingleTerminatorSerializer(terminator);
        ByteArrayInputStream inputStream = new ByteArrayInputStream("s\n\n".getBytes());

        try {
            byte[] bytes = serializer.deserialize(inputStream);
            assertEquals(1, bytes.length);
            assertEquals("s".getBytes()[0], bytes[0]);
            bytes = serializer.deserialize(inputStream);
            assertEquals(0, bytes.length);
        } finally {
            inputStream.close();
        }
    }
}
