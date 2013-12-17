/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.messaging.Message;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.MessageSequenceComparator;
import org.springframework.integration.annotation.Aggregator;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;

/**
 * @author Marius Bogoevici
 */
@Component("endpointWithCustomizedAnnotation")
public class TestAnnotatedEndpointWithCustomizedAggregator {

	private final ConcurrentMap<Object, Message<?>> aggregatedMessages = new ConcurrentHashMap<Object, Message<?>>();

	@Aggregator(
			inputChannel = "inputChannel",
			outputChannel = "outputChannel",
			discardChannel = "discardChannel",
			sendPartialResultsOnExpiry = true,
			sendTimeout = 98765432)
	public Message<?> aggregatingMethod(List<Message<?>> messages) {
		List<Message<?>> sortableList = new ArrayList<Message<?>>(messages);
		Collections.sort(sortableList, new MessageSequenceComparator());
		StringBuffer buffer = new StringBuffer();
		Object correlationId = null;
		for (Message<?> message : sortableList) {
			buffer.append(message.getPayload().toString());
			if (null == correlationId) {
				correlationId = new IntegrationMessageHeaderAccessor(message).getCorrelationId();
			}
		}
		Message<?> returnedMessage = new GenericMessage<String>(buffer.toString());
		aggregatedMessages.put(correlationId, returnedMessage);
		return returnedMessage;
	}

	public ConcurrentMap<Object, Message<?>> getAggregatedMessages() {
		return aggregatedMessages;
	}

}
