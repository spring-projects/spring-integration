/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import org.springframework.messaging.MessageHeaders;


/**
 * @author Stuart Williams
 * @since 4.2
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
