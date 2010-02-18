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

package org.springframework.integration.aggregator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHistoryEvent;
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
		AbstractMessageBarrierHandler<List<Message<?>>> {

	private static final String COMPONENT_TYPE_LABEL = "aggregator";


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
	protected MessageBarrier<List<Message<?>>> createMessageBarrier(Object correlationKey) {
		return new MessageBarrier<List<Message<?>>>(new ArrayList<Message<?>>(), correlationKey);
	}

	@Override
	protected void processBarrier(MessageBarrier<List<Message<?>>> barrier) {
        if (!barrier.isComplete() && !CollectionUtils.isEmpty(barrier.getMessages())) {
			if (this.completionStrategy.isComplete(barrier.getMessages())) {
				barrier.setComplete();
			}
		}
		if (barrier.isComplete()) {
			this.removeBarrier(barrier.getCorrelationKey());
			Message<?> result = this.aggregateMessages(barrier.getMessages());
			if (result != null) {
				if (result.getHeaders().getCorrelationId() == null) {
					result = MessageBuilder.fromMessage(result)
							.setCorrelationId(barrier.getCorrelationKey())
							.build();
				}
				this.sendReply(result, this.resolveReplyChannelFromMessage(barrier.getMessages().get(0)));
			}
		}
	}

	@Override
	protected void postProcessHistoryEvent(MessageHistoryEvent event) {
		event.setComponentType(COMPONENT_TYPE_LABEL);
	}

    protected abstract Message<?> aggregateMessages(List<Message<?>> messages);

}
