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

import java.util.Collection;

import org.springframework.integration.core.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReplyMessageHolder;

/**
 * Base class for Message-splitting handlers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageSplitter extends AbstractReplyProducingMessageHandler {

	@Override
	protected final void handleRequestMessage(Message<?> message, ReplyMessageHolder replyHolder) {
		Object result = this.splitMessage(message);
		if (result == null) {
			return;
		}
		Object correlationId = (message.getHeaders().getCorrelationId() != null) ? 
                message.getHeaders().getCorrelationId(): message.getHeaders().getId();
		if (result instanceof Collection) {
			Collection<?> items = (Collection<?>) result;
			int sequenceNumber = 0;
			int sequenceSize = items.size();
			for (Object item : items) {
				this.addReply(replyHolder, item, correlationId, ++sequenceNumber, sequenceSize);
			}
		}
		else if (result.getClass().isArray()) {
			Object[] items = (Object[]) result;
			int sequenceNumber = 0;
			int sequenceSize = items.length;
			for (Object item : items) {
				this.addReply(replyHolder, item, correlationId, ++sequenceNumber, sequenceSize);
			}
		}
		else {
			this.addReply(replyHolder, result, correlationId, 1, 1);
		}
	}

	private void addReply(ReplyMessageHolder replyHolder, Object item, Object correlationId, int sequenceNumber, int sequenceSize) {
		replyHolder.add(item).setCorrelationId(correlationId)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize);
	}

	/**
	 * Subclasses must override this method to split the received Message. The
	 * return value may be a Collection or Array. The individual elements may
	 * be Messages, but it is not necessary. If the elements are not Messages,
	 * each will be provided as the payload of a Message. It is also acceptable
	 * to return a single Object or Message. In that case, a single reply
	 * Message will be produced.
	 */
	protected abstract Object splitMessage(Message<?> message);

}
