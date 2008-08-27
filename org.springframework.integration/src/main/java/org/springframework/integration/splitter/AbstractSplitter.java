/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.splitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHeaders;

/**
 * @author Mark Fisher
 */
public abstract class AbstractSplitter implements Splitter {

	public List<Message<?>> split(Message<?> message) {
		Object result = this.splitMessage(message);
		MessageHeaders requestHeaders = message.getHeaders();
		List<Message<?>> results = new ArrayList<Message<?>>();
		if (result instanceof Collection) {
			Collection<?> items = (Collection<?>) result;
			int sequenceNumber = 0;
			int sequenceSize = items.size();
			for (Object item : items) {
				results.add(this.createSplitMessage(item, requestHeaders, ++sequenceNumber, sequenceSize));
			}
		}
		else if (result.getClass().isArray()) {
			Object[] items = (Object[]) result;
			int sequenceNumber = 0;
			int sequenceSize = items.length;
			for (Object item : items) {
				results.add(this.createSplitMessage(item, requestHeaders, ++sequenceNumber, sequenceSize));
			}
		}
		else {
			results.add(this.createSplitMessage(result, requestHeaders, 1, 1));
		}
		if (results.isEmpty()) {
			return null;
		}
		return results;
	}

	protected abstract Object splitMessage(Message<?> message);

	private Message<?> createSplitMessage(Object item, MessageHeaders requestHeaders, int sequenceNumber, int sequenceSize) {
		if (item instanceof Message<?>) {
			return setSplitMessageHeaders(MessageBuilder.fromMessage((Message<?>) item),
					requestHeaders.getId(), sequenceNumber, sequenceSize);
		}
		return setSplitMessageHeaders(MessageBuilder.fromPayload(item),
				requestHeaders.getId(), sequenceNumber, sequenceSize);
	}

	private Message<?> setSplitMessageHeaders(MessageBuilder<?> builder, Object requestMessageId, int sequenceNumber, int sequenceSize) {
		return builder.setCorrelationId(requestMessageId)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize).build();
	}

}
