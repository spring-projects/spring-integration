/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.gemfire.inbound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gemstone.gemfire.cache.CacheClosedException;
import com.gemstone.gemfire.cache.Operation;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEvent;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventListener;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventQueue;
import com.gemstone.gemfire.cache.asyncqueue.AsyncEventQueueFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.endpoint.ExpressionMessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An inbound endpoint that registers a GemFire {@link AsyncEventListener}
 * to a partitioned region and publishes messages based on data applied to
 * the region. This implementation differs from {@link CacheListeningMessageProducer}
 * because it uses an {@link AsyncEventQueue} to offload processing to a dedicated thread.
 * <p>
 * The default supported operations are
 * <ul>
 *     <li>{@link Operation#CREATE}</li>
 *     <li>{@link Operation#UPDATE}</li>
 *     <li>{@link Operation#PUTALL_CREATE}</li>
 *     <li>{@link Operation#PUTALL_UPDATE}</li>
 * </ul>
 * A SpEL expression may be provided to generate a Message payload by evaluating
 * that expression against the {@link AsyncEvent} instance as the root object.
 * If no {@code payloadExpression} is provided, the {@code AsyncEvent} itself
 * will be the payload.
 *
 * @author Patrick Peralta
 */
public class AsyncEventListeningMessageProducer extends ExpressionMessageProducerSupport {

	public static final String QUEUE_POSTFIX = "-queue";

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Region<?, ?> region;

	private final AsyncEventQueueFactory queueFactory;

	private final AsyncEventListener listener;

	private final String queueId;

	private volatile Set<Operation> supportedOperations =
			new HashSet<Operation>(Arrays.asList(
					Operation.CREATE,
					Operation.UPDATE,
					Operation.PUTALL_CREATE,
					Operation.PUTALL_UPDATE));


	/**
	 * Construct a {@code AsyncEventListeningMessageProducer}.
	 *
	 * @param region the region that contains messages
	 * @param queueFactory queue factory used to create write-behind queue
	 */
	public AsyncEventListeningMessageProducer(Region<?, ?> region, AsyncEventQueueFactory queueFactory) {
		Assert.notNull(region, "region must not be null");
		Assert.notNull(queueFactory, "queueFactory must not be null");

		this.region = region;
		this.queueFactory = queueFactory;
		this.listener = new MessageProducingAsyncEventListener();
		this.queueId = region.getName() + QUEUE_POSTFIX;
	}

	/**
	 * Set the list of operations that will cause a message to be published.
	 *
	 * @param operations supported operations
	 */
	public void setSupportedOperations(Operation... operations) {
		Assert.notEmpty(operations, "operations must not be empty");
		this.supportedOperations = new HashSet<Operation>(Arrays.asList(operations));
	}

	@Override
	public String getComponentType() {
		return "gemfire:inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		if (logger.isInfoEnabled()) {
			logger.info("adding MessageProducingAsyncEventListener to GemFire Region '" +
					this.region.getName() + "'");
		}

		this.queueFactory.create(queueId, this.listener);
		this.region.getAttributesMutator().addAsyncEventQueueId(this.queueId);
	}

	@Override
	protected void doStop() {
		if (logger.isInfoEnabled()) {
			logger.info("removing MessageProducingAsyncEventListener from GemFire Region '" +
					this.region.getName() + "'");
		}
		try {
			this.region.getAttributesMutator().removeAsyncEventQueueId(this.queueId);
		}
		catch (CacheClosedException e) {
			if (logger.isDebugEnabled()){
				logger.debug(e.getMessage(),e);
			}
		}
	}


	/**
	 * Implementation of {@link AsyncEventListener} that publishes messages
	 * to a channel.
	 */
	private class MessageProducingAsyncEventListener implements AsyncEventListener {

		@Override
		public boolean processEvents(List<AsyncEvent> events) {
			for (AsyncEvent event : events) {
				if (supportedOperations.contains(event.getOperation())) {
					processEvent(event);
				}
			}
			return true;
		}

		private void processEvent(AsyncEvent event) {
			this.publish(evaluatePayloadExpression(event));
		}

		private void publish(Object object) {
			Message<?> message = object instanceof Message
					? (Message<?>) object
					: getMessageBuilderFactory().withPayload(object).build();
			sendMessage(message);
		}

		@Override
		public void close() {
		}
	}

}
