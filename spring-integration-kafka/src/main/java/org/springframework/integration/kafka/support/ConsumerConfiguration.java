/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.support;

import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.MessagingException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Soby Chacko
 */
public class ConsumerConfiguration {
    private static final Log LOGGER = LogFactory.getLog(ConsumerConfiguration.class);

    private final ConsumerMetadata consumerMetadata;
    private final ConsumerConnectionProvider consumerConnectionProvider;
    private final MessageLeftOverTracker messageLeftOverTracker;
    private ConsumerConnector consumerConnector;
    private volatile int count = 0;
    private int maxMessages = 1;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public ConsumerConfiguration(final ConsumerMetadata consumerMetadata,
                                 final ConsumerConnectionProvider consumerConnectionProvider,
                                 final MessageLeftOverTracker messageLeftOverTracker) {
        this.consumerMetadata = consumerMetadata;
        this.consumerConnectionProvider = consumerConnectionProvider;
        this.messageLeftOverTracker = messageLeftOverTracker;
    }

    public ConsumerMetadata getConsumerMetadata() {
        return consumerMetadata;
    }

    public Map<String, Map<Integer, List<Object>>> receive() {
        count = messageLeftOverTracker.getCurrentCount();

        final List<Callable<List<MessageAndMetadata>>> tasks = new LinkedList<Callable<List<MessageAndMetadata>>>();
        final Object lock = new Object();

        final Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = getConsumerMapWithMessageStreams();
        for (final List<KafkaStream<byte[], byte[]>> streams : consumerMap.values()) {
            for (final KafkaStream<byte[], byte[]> stream : streams) {
                tasks.add(new Callable<List<MessageAndMetadata>>() {
                    @Override
                    public List<MessageAndMetadata> call() throws Exception {
                        final List<MessageAndMetadata> rawMessages = new ArrayList<MessageAndMetadata>();
                        try {
                            while (count < maxMessages) {
                                final MessageAndMetadata messageAndMetadata = stream.iterator().next();
                                synchronized (lock) {
                                    if (count < maxMessages) {
                                        rawMessages.add(messageAndMetadata);
                                        count++;
                                    } else {
                                        messageLeftOverTracker.addMessageAndMetadata(messageAndMetadata);
                                    }
                                }
                            }
                        } catch (ConsumerTimeoutException cte) {
                            LOGGER.info("Consumer timed out");
                        }
                        return rawMessages;
                    }
                });
            }
        }

        return executeTasks(tasks);
    }

    private Map<String, Map<Integer, List<Object>>> executeTasks(final List<Callable<List<MessageAndMetadata>>> tasks) {

        final Map<String, Map<Integer, List<Object>>> messages = new ConcurrentHashMap<String, Map<Integer, List<Object>>>();
        messages.putAll(getLeftOverMessageMap());

        try {
            for (final Future<List<MessageAndMetadata>> result : executorService.invokeAll(tasks)) {
                if (!result.get().isEmpty()) {
                    final String topic = result.get().get(0).topic();
                    if (!messages.containsKey(topic)) {
                        messages.put(topic, getPayload(result.get()));
                    } else {

                        final Map<Integer, List<Object>> existingPayloadMap = messages.get(topic);
                        getPayload(result.get(), existingPayloadMap);
                    }
                }
            }
        } catch (Exception e) {
            throw new MessagingException("Consuming from Kafka failed", e);
        }

        if (messages.isEmpty()) {
            return null;
        }

        return messages;
    }

    private Map<String, Map<Integer, List<Object>>> getLeftOverMessageMap() {

        final Map<String, Map<Integer, List<Object>>> messages = new ConcurrentHashMap<String, Map<Integer, List<Object>>>();

        for (final MessageAndMetadata mamd : messageLeftOverTracker.getMessageLeftOverFromPreviousPoll()) {
            final String topic = mamd.topic();

            if (!messages.containsKey(topic)) {
                final List<MessageAndMetadata> l = new ArrayList<MessageAndMetadata>();
                l.add(mamd);
                messages.put(topic, getPayload(l));
            } else {
                final Map<Integer, List<Object>> existingPayloadMap = messages.get(topic);
                final List<MessageAndMetadata> l = new ArrayList<MessageAndMetadata>();
                l.add(mamd);
                getPayload(l, existingPayloadMap);
            }
        }
        messageLeftOverTracker.clearMessagesLeftOver();
        return messages;
    }

    private Map<Integer, List<Object>> getPayload(final List<MessageAndMetadata> messageAndMetadatas) {
        final Map<Integer, List<Object>> payloadMap = new ConcurrentHashMap<Integer, List<Object>>();

        for (final MessageAndMetadata messageAndMetadata : messageAndMetadatas) {
            if (!payloadMap.containsKey(messageAndMetadata.partition())) {
                final List<Object> payload = new ArrayList<Object>();
                payload.add(messageAndMetadata.message());
                payloadMap.put(messageAndMetadata.partition(), payload);
            } else {
                final List<Object> payload = payloadMap.get(messageAndMetadata.partition());
                payload.add(messageAndMetadata.message());
            }

        }

        return payloadMap;
    }

    private void getPayload(final List<MessageAndMetadata> messageAndMetadatas, final Map<Integer, List<Object>> existingPayloadMap) {
        for (final MessageAndMetadata messageAndMetadata : messageAndMetadatas) {
            if (!existingPayloadMap.containsKey(messageAndMetadata.partition())) {
                final List<Object> payload = new ArrayList<Object>();
                payload.add(messageAndMetadata.message());
                existingPayloadMap.put(messageAndMetadata.partition(), payload);
            } else {
                final List<Object> payload = existingPayloadMap.get(messageAndMetadata.partition());
                payload.add(messageAndMetadata.message());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, List<KafkaStream<byte[], byte[]>>> getConsumerMapWithMessageStreams() {
        if (consumerMetadata.getValueDecoder() != null) {
            return getConsumerConnector().createMessageStreams(
                    consumerMetadata.getTopicStreamMap(),
                    consumerMetadata.getValueDecoder(),
                    consumerMetadata.getValueDecoder());
        }

        return getConsumerConnector().createMessageStreams(consumerMetadata.getTopicStreamMap());
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(final int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public ConsumerConnector getConsumerConnector() {
        if (consumerConnector == null) {
            consumerConnector = consumerConnectionProvider.getConsumerConnector();
        }

        return consumerConnector;
    }
}
