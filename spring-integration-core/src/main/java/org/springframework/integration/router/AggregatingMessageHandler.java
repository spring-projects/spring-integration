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

package org.springframework.integration.router;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that waits for a <em>complete</em>
 * group of {@link Message Messages} to arrive and then delegates to an
 * {@link Aggregator} to combine them into a single {@link Message}.
 * <p>
 * Each {@link Message} that is received by this handler will be associated with
 * a group based upon the '<code>correlationId</code>' property of its
 * header. If no such property is available, a {@link MessageHandlingException}
 * will be thrown.
 * <p>
 * The default strategy for determining whether a group is complete is based on
 * the '<code>sequenceSize</code>' property of the header. Alternatively, a
 * custom implementation of the {@link RoutingBarrierCompletionStrategy} may be
 * provided.
 * <p>
 * The '<code>timeout</code>' value determines how long to wait for the
 * complete group after the arrival of the first {@link Message} of the group.
 * The default value is 1 minute. If the timeout elapses prior to completion,
 * the handler will throw a {@link MessageHandlingException} by default. To
 * prevent the exception and aggregate the group even when incomplete, set the
 * '<code>shouldFailOnTimeout</code>' property to '<code>false</code>'.
 * 
 * @author Mark Fisher
 */
public class AggregatingMessageHandler implements MessageHandler {

	private long timeout = 60000;

	private boolean shouldFailOnTimeout = true;

	private Aggregator aggregator;

	private RoutingBarrierCompletionStrategy completionStrategy = new SequenceSizeCompletionStrategy();

	private ConcurrentHashMap<Object, RoutingBarrier> barriers = new ConcurrentHashMap<Object, RoutingBarrier>();


	/**
	 * Create a handler that delegates to the provided aggregator to combine a
	 * group of messages into a single message.
	 */
	public AggregatingMessageHandler(Aggregator aggregator) {
		Assert.notNull(aggregator, "'aggregator' must not be null");
		this.aggregator = aggregator;
	}


	/**
	 * Strategy to determine whether the group of messages is complete.
	 */
	public void setCompletionStrategy(RoutingBarrierCompletionStrategy completionStrategy) {
		Assert.notNull(completionStrategy, "'completionStrategy' must not be null");
		this.completionStrategy = completionStrategy;
	}

	/**
	 * Maximum time to wait (in milliseconds) for the completion strategy to
	 * become true.
	 */
	public void setTimeout(long timeout) {
		Assert.isTrue(timeout >= 0, "'timeout' must not be negative");
		this.timeout = timeout;
	}

	/**
	 * Specify whether this handler should throw a {@link MessageHandlingException}
	 * when a message group does not reach completion within the allotted time. The
	 * default is '<code>true</code>'. Setting this to '<code>false</code>' will cause
	 * the {@link Aggregator} to be invoked even when the group is incomplete.
	 */
	public void setShouldFailOnTimeout(boolean setShouldFailOnTimeout) {
		this.shouldFailOnTimeout = setShouldFailOnTimeout;
	}

	public Message<?> handle(Message<?> message) {
		Object correlationId = message.getHeader().getCorrelationId();
		if (correlationId == null) {
			throw new MessageHandlingException(this.getClass().getSimpleName() +
					" requires the 'correlationId' property");
		}
		RoutingBarrier barrier = barriers.putIfAbsent(correlationId, new RoutingBarrier(this.completionStrategy));
		if (barrier == null) {
			try {
				barrier = barriers.get(correlationId);
				barrier.addMessage(message);
				if (!barrier.waitForCompletion(this.timeout) && this.shouldFailOnTimeout) {
					throw new MessageHandlingException("aggregation did not complete "
							+ "within the allotted time limit of " + this.timeout + " milliseconds");
				}
				Message<?> result = aggregator.aggregate(barrier.getMessages());
				return result;
			}
			finally {
				this.barriers.remove(correlationId);
			}
		}
		else {
			barriers.get(correlationId).addMessage(message);
			return null;
		}
	}

}
