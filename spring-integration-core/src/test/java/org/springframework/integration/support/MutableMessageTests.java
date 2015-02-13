package org.springframework.integration.support;

import org.junit.Test;
import org.springframework.messaging.MessageHeaders;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;


/**
 * @author swilliams
 */
public class MutableMessageTests {

    @Test
    public void testMessageIdTimestampRemains() {

        UUID uuid = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();

        Object payload = new Object();
        Map<String, Object> headerMap = new HashMap<>();

        headerMap.put(MessageHeaders.ID, uuid);
        headerMap.put(MessageHeaders.TIMESTAMP, timestamp);

        MutableMessage<Object> mutableMessage = new MutableMessage<>(payload, headerMap);
        MutableMessageHeaders headers = mutableMessage.getHeaders();

        assertThat(headers.getRawHeaders(), hasEntry(MessageHeaders.ID, (Object) uuid));
        assertThat(headers.getRawHeaders(), hasEntry(MessageHeaders.TIMESTAMP, (Object) timestamp));
    }

    @Test
    public void testMessageHeaderIsSettable() {

        Object payload = new Object();
        Map<String, Object> headerMap = new HashMap<>();
        Map<String, Object> additional = new HashMap<>();

        MutableMessage<Object> mutableMessage = new MutableMessage<>(payload, headerMap);
        MutableMessageHeaders headers = mutableMessage.getHeaders();

        // Should not throw an UnsupportedOperationException
        headers.put("foo", "bar");
        headers.put("eep", "bar");
        headers.remove("eep");
        headers.putAll(additional);

        assertThat(headers.getRawHeaders(), hasEntry("foo", (Object) "bar"));
    }

}
