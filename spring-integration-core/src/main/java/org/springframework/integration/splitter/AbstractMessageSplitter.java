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

package org.springframework.integration.splitter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Base class for Message-splitting handlers.
 * 
 * @author Mark Fisher
 * @author Dave Syer
 */
public abstract class AbstractMessageSplitter extends AbstractReplyProducingMessageHandler {

	private boolean applySequence = true;

	/**
	 * Set the applySequence flag to the specified value. Defaults to true.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected final Object handleRequestMessage(Message<?> message) {
		Object result = this.splitMessage(message);
		// return null if 'null', empty Collection or empty Array
		if (result == null || (result instanceof Collection && CollectionUtils.isEmpty((Collection) result))
				|| (result.getClass().isArray() && ObjectUtils.isEmpty((Object[]) result))) {
			return null;
		}
		MessageHeaders headers = message.getHeaders();
		Object correlationId = headers.getId();
		List<MessageBuilder<?>> messageBuilders = new ArrayList<MessageBuilder<?>>();
		if (result instanceof Collection) {
			Collection<?> items = (Collection<?>) result;
			int sequenceNumber = 0;
			int sequenceSize = items.size();
			for (Object item : items) {
				messageBuilders.add(this.createBuilder(item, headers, correlationId, ++sequenceNumber, sequenceSize));
			}
		}
		else if (result.getClass().isArray()) {
			Object[] items = (Object[]) result;
			int sequenceNumber = 0;
			int sequenceSize = items.length;
			for (Object item : items) {
				messageBuilders.add(this.createBuilder(item, headers, correlationId, ++sequenceNumber, sequenceSize));
			}
		}
		else {
			messageBuilders.add(this.createBuilder(result, headers, correlationId, 1, 1));
		}
		return messageBuilders;
	}

	@SuppressWarnings( { "unchecked", "rawtypes" })
	private MessageBuilder createBuilder(Object item, MessageHeaders headers, Object correlationId, int sequenceNumber,
			int sequenceSize) {
		MessageBuilder builder;
		if (item instanceof Message) {
			builder = MessageBuilder.fromMessage((Message) item);
		}
		else {
			builder = MessageBuilder.withPayload(item);
			builder.copyHeaders(headers);
		}
		if (this.applySequence) {
			builder.pushSequenceDetails(correlationId, sequenceNumber, sequenceSize);
		}
		return builder;
	}

	@Override
	public String getComponentType() {
		return "splitter";
	}

	/**
	 * Subclasses must override this method to split the received Message. The return value may be a Collection or
	 * Array. The individual elements may be Messages, but it is not necessary. If the elements are not Messages, each
	 * will be provided as the payload of a Message. It is also acceptable to return a single Object or Message. In that
	 * case, a single reply Message will be produced.
	 */
	protected abstract Object splitMessage(Message<?> message);

}
