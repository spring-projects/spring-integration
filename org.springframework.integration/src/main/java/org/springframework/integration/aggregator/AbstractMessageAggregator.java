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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A base class for aggregating a group of Messages into a single Message.
 * Extends {@link AbstractMessageBarrierHandler} and waits for a
 * <em>complete</em> group of {@link Message Messages} to arrive. Subclasses
 * must provide the implementation of the {@link #aggregateMessages(List)}
 * method to combine the group of Messages into a single {@link Message}.
 * 
 * <p>
 * The default strategy for determining whether a group is complete is based on
 * the '<code>sequenceSize</code>' property of the header. Alternatively, a
 * custom implementation of the {@link CompletionStrategy} may be provided.
 * 
 * <p>
 * All considerations regarding <code>timeout</code> and grouping by
 * <code>correlationId</code> from {@link AbstractMessageBarrierHandler} apply
 * here as well.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class AbstractMessageAggregator extends
		AbstractMessageBarrierHandler<Map<Object, Message<?>>, Object> {

	private volatile CompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();

	
	/**
	 * Strategy to determine whether the group of messages is complete.
	 */
	public void setCompletionStrategy(CompletionStrategy completionStrategy) {
		Assert.notNull(completionStrategy,
				"'completionStrategy' must not be null");
		this.completionStrategy = completionStrategy;
	}

	@Override
	protected MessageBarrier<Map<Object, Message<?>>, Object> createMessageBarrier() {
		return new MessageBarrier<Map<Object, Message<?>>, Object>(new LinkedHashMap<Object, Message<?>>());
	}

	@Override
	protected void processBarrier(MessageBarrier<Map<Object, Message<?>>, Object> barrier) {
		ArrayList<Message<?>> messageList = new ArrayList<Message<?>>(barrier.getMessages().values());
		if (!barrier.isComplete() && !CollectionUtils.isEmpty(messageList)) {
			if (this.completionStrategy.isComplete(messageList)) {
				barrier.setComplete();
			}
		}
		if (barrier.isComplete()) {
			this.removeBarrier(barrier.getCorrelationId());
			Message<?> result = this.aggregateMessages(messageList);
			if (result != null) {
				if (result.getHeaders().getCorrelationId() == null) {
					result = MessageBuilder.fromMessage(result)
							.setCorrelationId(barrier.getCorrelationId())
							.build();
				}
				this.sendReply(result, this.resolveReplyChannelFromMessage(messageList.get(0)));
			}
		}
	}
	
	@Override
	protected boolean canAddMessage(Message<?> message, MessageBarrier<Map<Object, Message<?>>, Object> barrier) {
		if (!super.canAddMessage(message, barrier)) {
			return false;
		}
		if (barrier.messages.containsKey(message.getHeaders().getId())) {
			logger.debug("The barrier has received message: " + message
					+ ", but it already contains a similar message: " 
					+ barrier.getMessages().get(message.getHeaders().getId()));
			return false;
		}
		return true;
	}
	
	@Override
	protected void doAddMessage(Message<?> message, MessageBarrier<Map<Object, Message<?>>, Object> barrier) {
		barrier.getMessages().put(message.getHeaders().getId(), message);
	}

	protected abstract Message<?> aggregateMessages(List<Message<?>> messages);

}
