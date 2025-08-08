/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.MessageSequenceComparator;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@MessageEndpoint("endpointWithDefaultAnnotation")
public class TestAnnotatedEndpointWithDefaultAggregator {

	private final ConcurrentMap<Object, Message<?>> aggregatedMessages = new ConcurrentHashMap<Object, Message<?>>();

	@Aggregator(inputChannel = "inputChannel")
	public Message<?> aggregatingMethod(List<Message<?>> messages) {
		List<Message<?>> sortableList = new ArrayList<>(messages);
		Collections.sort(sortableList, new MessageSequenceComparator());
		StringBuffer buffer = new StringBuffer();
		Object correlationId = null;
		for (Message<?> message : sortableList) {
			buffer.append(message.getPayload().toString());
			if (null == correlationId) {
				correlationId = new IntegrationMessageHeaderAccessor(message).getCorrelationId();
			}
		}
		Message<?> returnedMessage = new GenericMessage<>(buffer.toString());
		this.aggregatedMessages.put(correlationId, returnedMessage);
		return returnedMessage;
	}

	public ConcurrentMap<Object, Message<?>> getAggregatedMessages() {
		return this.aggregatedMessages;
	}

}
