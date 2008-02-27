/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.router.Aggregator;
import org.springframework.integration.router.MessageSequenceComparator;

/**
 * @author Marius Bogoevici
 */
public class TestAggregator implements Aggregator {

	private final ConcurrentMap<Object, Message<?>> aggregatedMessages = new ConcurrentHashMap<Object, Message<?>>();


	public Message<?> aggregate(List<Message<?>> messages) {
		List<Message<?>> sortableList = new ArrayList<Message<?>>(messages);
		Collections.sort(sortableList, new MessageSequenceComparator());
		StringBuffer buffer = new StringBuffer();
		for (Message<?> message : sortableList) {
			buffer.append(message.getPayload().toString());
		}
		Message<?> returnedMessage =  new StringMessage(buffer.toString());
		aggregatedMessages.put(messages.get(0).getHeader().getCorrelationId(), returnedMessage);
		return returnedMessage;
	}

	public ConcurrentMap<Object, Message<?>> getAggregatedMessages() {
		return aggregatedMessages;
	}

}
