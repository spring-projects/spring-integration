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
import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * The Default Message Aggregator implementation that combines a group of
 * messages into a single message containing a {@link List} of all payloads. The
 * elements of the List are in order of their receiving. Any MessageHeader value
 * is ignored except the <code>correlationId</code>.
 * 
 * <p>
 * n The default strategy for determining whether a group is complete is based
 * on the '<code>sequenceSize</code>' property of the header. Alternatively, a
 * custom implementation of the {@link CompletionStrategy} may be provided.
 * </p>
 * <p>
 * All considerations regarding <code>timeout</code> and grouping by
 * <code>correlationId</code> from {@link AbstractMessageBarrierHandler} apply
 * here as well.
 * </p>
 * 
 * @author Alex Peters
 * 
 */
public class DefaultMessageAggregator extends AbstractMessageAggregator {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Message<?> aggregateMessages(List<Message<?>> messages) {
		List<Object> payloads = new ArrayList<Object>(messages.size());
		for (Message<?> message : messages) {
			payloads.add(message.getPayload());
		}
		return MessageBuilder.withPayload(payloads).build();
	}

}
