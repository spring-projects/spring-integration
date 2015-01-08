/*
 * Copyright 2002-2015 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */

package org.springframework.integration.kafka.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @author Artem Bilan
 * @since 0.5
 */
public class ConsumerConfiguration<K, V> {
	private static final Log LOGGER = LogFactory.getLog(ConsumerConfiguration.class);

	private final ConsumerMetadata<K, V> consumerMetadata;

	private final ConsumerConnectionProvider consumerConnectionProvider;

	private final MessageLeftOverTracker<K, V> messageLeftOverTracker;

	private ConsumerConnector consumerConnector;

	private volatile int count = 0;

	private int maxMessages = 1;

	private Collection<List<KafkaStream<K, V>>> consumerMessageStreams;

	private ExecutorService executorService = Executors.newCachedThreadPool();

	private boolean executorExplicitlySet;

	private volatile boolean stopped;

	public ConsumerConfiguration(final ConsumerMetadata<K, V> consumerMetadata,
			final ConsumerConnectionProvider consumerConnectionProvider,
			final MessageLeftOverTracker<K, V> messageLeftOverTracker) {
		this.consumerMetadata = consumerMetadata;
		this.consumerConnectionProvider = consumerConnectionProvider;
		this.messageLeftOverTracker = messageLeftOverTracker;
	}

	public void setExecutor(Executor executor) {
		boolean isExecutorService = executor instanceof ExecutorService;
		boolean isThreadPoolTaskExecutor = executor instanceof ThreadPoolTaskExecutor;
		Assert.isTrue(isExecutorService || isThreadPoolTaskExecutor);
		if (isExecutorService) {
			this.executorService = (ExecutorService) executor;
		}
		else {
			this.executorService = ((ThreadPoolTaskExecutor) executor).getThreadPoolExecutor();
		}
		this.executorExplicitlySet = true;
	}

	public ConsumerMetadata<K, V> getConsumerMetadata() {
		return consumerMetadata;
	}

	public Map<String, Map<Integer, List<Object>>> receive() {
		count = messageLeftOverTracker.getCurrentCount();
		final Object lock = new Object();

		final List<Callable<List<MessageAndMetadata<K, V>>>> tasks = new LinkedList<Callable<List<MessageAndMetadata<K, V>>>>();

		for (final List<KafkaStream<K, V>> streams : createConsumerMessageStreams()) {
			for (final KafkaStream<K, V> stream : streams) {
				tasks.add(new Callable<List<MessageAndMetadata<K, V>>>() {
					@Override
					public List<MessageAndMetadata<K, V>> call() throws Exception {
						final List<MessageAndMetadata<K, V>> rawMessages = new ArrayList<MessageAndMetadata<K, V>>();
						try {
							while (count < maxMessages) {
								final MessageAndMetadata<K, V> messageAndMetadata = stream.iterator().next();
								synchronized (lock) {
									if (count < maxMessages) {
										rawMessages.add(messageAndMetadata);
										count++;
									}
									else {
										messageLeftOverTracker.addMessageAndMetadata(messageAndMetadata);
									}
								}
							}
						}
						catch (ConsumerTimeoutException cte) {
							LOGGER.debug("Consumer timed out");
						}
						return rawMessages;
					}
				});
			}
		}
		return executeTasks(tasks);
	}

	private Map<String, Map<Integer, List<Object>>> executeTasks(
			final List<Callable<List<MessageAndMetadata<K, V>>>> tasks) {

		final Map<String, Map<Integer, List<Object>>> messages = new ConcurrentHashMap<String, Map<Integer, List<Object>>>();
		messages.putAll(getLeftOverMessageMap());

		try {
			for (final Future<List<MessageAndMetadata<K, V>>> result : this.executorService.invokeAll(tasks)) {
				if (!result.get().isEmpty()) {
					final String topic = result.get().get(0).topic();
					if (!messages.containsKey(topic)) {
						messages.put(topic, getPayload(result.get()));
					}
					else {

						final Map<Integer, List<Object>> existingPayloadMap = messages.get(topic);
						getPayload(result.get(), existingPayloadMap);
					}
				}
			}
		}
		catch (Exception e) {
			if (!this.stopped) {
				throw new MessagingException("Consuming from Kafka failed", e);
			}
			else {
				LOGGER.warn("Consuming from Kafka failed", e);
			}
		}

		if (messages.isEmpty()) {
			return null;
		}

		return messages;
	}

	private Map<String, Map<Integer, List<Object>>> getLeftOverMessageMap() {

		final Map<String, Map<Integer, List<Object>>> messages = new ConcurrentHashMap<String, Map<Integer, List<Object>>>();

		for (final MessageAndMetadata<K, V> mamd : messageLeftOverTracker.getMessageLeftOverFromPreviousPoll()) {
			final String topic = mamd.topic();

			if (!messages.containsKey(topic)) {
				final List<MessageAndMetadata<K, V>> l = new ArrayList<MessageAndMetadata<K, V>>();
				l.add(mamd);
				messages.put(topic, getPayload(l));
			}
			else {
				final Map<Integer, List<Object>> existingPayloadMap = messages.get(topic);
				final List<MessageAndMetadata<K, V>> l = new ArrayList<MessageAndMetadata<K, V>>();
				l.add(mamd);
				getPayload(l, existingPayloadMap);
			}
		}
		messageLeftOverTracker.clearMessagesLeftOver();
		return messages;
	}

	private Map<Integer, List<Object>> getPayload(final List<MessageAndMetadata<K, V>> messageAndMetadatas) {
		final Map<Integer, List<Object>> payloadMap = new ConcurrentHashMap<Integer, List<Object>>();

		for (final MessageAndMetadata<K, V> messageAndMetadata : messageAndMetadatas) {
			if (!payloadMap.containsKey(messageAndMetadata.partition())) {
				final List<Object> payload = new ArrayList<Object>();
				payload.add(messageAndMetadata.message());
				payloadMap.put(messageAndMetadata.partition(), payload);
			}
			else {
				final List<Object> payload = payloadMap.get(messageAndMetadata.partition());
				payload.add(messageAndMetadata.message());
			}

		}

		return payloadMap;
	}

	private void getPayload(final List<MessageAndMetadata<K, V>> messageAndMetadatas,
			final Map<Integer, List<Object>> existingPayloadMap) {
		for (final MessageAndMetadata<K, V> messageAndMetadata : messageAndMetadatas) {
			if (!existingPayloadMap.containsKey(messageAndMetadata.partition())) {
				final List<Object> payload = new ArrayList<Object>();
				payload.add(messageAndMetadata.message());
				existingPayloadMap.put(messageAndMetadata.partition(), payload);
			}
			else {
				final List<Object> payload = existingPayloadMap.get(messageAndMetadata.partition());
				payload.add(messageAndMetadata.message());
			}
		}
	}

	private Collection<List<KafkaStream<K, V>>> createConsumerMessageStreams() {
		if (consumerMessageStreams == null) {
			if (!(consumerMetadata.getTopicStreamMap() == null || consumerMetadata.getTopicStreamMap().isEmpty())) {
				consumerMessageStreams = createMessageStreamsForTopic().values();
			}
			else {
				consumerMessageStreams = new ArrayList<List<KafkaStream<K, V>>>();
				consumerMessageStreams.add(createMessageStreamsForTopicFilter());
			}
		}
		return consumerMessageStreams;
	}

	public Map<String, List<KafkaStream<K, V>>> createMessageStreamsForTopic() {
		return getConsumerConnector().createMessageStreams(consumerMetadata.getTopicStreamMap(),
				consumerMetadata.getKeyDecoder(), consumerMetadata.getValueDecoder());
	}

	public List<KafkaStream<K, V>> createMessageStreamsForTopicFilter() {
		List<KafkaStream<K, V>> messageStream = new ArrayList<KafkaStream<K, V>>();
		TopicFilterConfiguration topicFilterConfiguration = consumerMetadata.getTopicFilterConfiguration();
		if (topicFilterConfiguration != null) {
			messageStream = getConsumerConnector().createMessageStreamsByFilter(
					topicFilterConfiguration.getTopicFilter(), topicFilterConfiguration.getNumberOfStreams(),
					consumerMetadata.getKeyDecoder(), consumerMetadata.getValueDecoder());
		}
		else {
			LOGGER.warn("No Topic Filter Configuration defined");
		}

		return messageStream;
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

	public void shutdown() {
		this.stopped = true;
		if (!this.executorExplicitlySet) {
			this.executorService.shutdownNow();
		}
		getConsumerConnector().shutdown();
	}

}
