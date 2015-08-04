/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.handler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * A pair of message handlers; the first suspends the thread until the second receives
 * a corresponding message.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class SuspendingMessageHandler extends AbstractReplyProducingMessageHandler {

	private final ReleasingMessageHandler releasingHandler;

	private final ConcurrentHashMap<Object, BlockingQueue<Message<?>>> suspensions =
			new ConcurrentHashMap<Object, BlockingQueue<Message<?>>>();

	private final ConcurrentHashMap<Object, Thread> inProcess = new ConcurrentHashMap<Object, Thread>();

	private final long timeout;

	private final CorrelationStrategy correlationStrategy;

	public SuspendingMessageHandler(long timeout) {
		this(new ReleasingMessageHandler(),
				new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID), timeout);
	}

	public SuspendingMessageHandler(ReleasingMessageHandler releaser, long timeout) {
		this(releaser, new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID),
				timeout);
	}

	public SuspendingMessageHandler(ReleasingMessageHandler releaser, CorrelationStrategy correlationStrategy,
			long timeout) {
		this.correlationStrategy = correlationStrategy;
		this.timeout = timeout;
		this.releasingHandler = releaser;
		this.releasingHandler.setCorrelationStrategy(this.correlationStrategy);
		this.releasingHandler.setSuspensions(this.suspensions);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Object key = SuspendingMessageHandler.this.correlationStrategy.getCorrelationKey(requestMessage);
		if (key == null) {
			throw new MessagingException(requestMessage, "Correlation Strategy returned null");
		}
		Thread existing = this.inProcess.putIfAbsent(key, Thread.currentThread());
		if (existing != null) {
			throw new MessagingException(requestMessage, "Correlation key ("
					+ key + ") is already in use by " + existing.getName());
		}
		BlockingQueue<Message<?>> blockingQueue = createOrObtainQueue(this.suspensions, key);
		try {
			Message<?> release = blockingQueue.poll(this.timeout, TimeUnit.MILLISECONDS);
			if (release != null) {
				this.suspensions.remove(key);
				if (release.getPayload() instanceof Throwable) {
					throw new MessagingException(requestMessage, "Releasing flow returned a throwable",
							(Throwable) release.getPayload());
				}
				else {
					return buildResult(requestMessage, release);
				}
			}
			else {
				synchronized(blockingQueue) {
					if (!blockingQueue.offer(requestMessage)) {
						// second chance
						return buildResult(requestMessage, blockingQueue.poll());
					}
				}
				return null;
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException(requestMessage, "Interrupted while waiting for release", e);
		}
		finally {
			this.inProcess.remove(key);
		}
	}

	private Object buildResult(Message<?> requestMessage, Message<?> release) {
		Object[] result = new Object[2];
		result[0] = requestMessage.getPayload();
		result[1] = release;
		return result;
	}

	private static BlockingQueue<Message<?>> createOrObtainQueue(
			ConcurrentHashMap<Object, BlockingQueue<Message<?>>> suspensions, Object key) {
		BlockingQueue<Message<?>> blockingQueue = new ArrayBlockingQueue<Message<?>>(1);
		BlockingQueue<Message<?>> existing = suspensions.putIfAbsent(key, blockingQueue);
		if (existing != null) {
			blockingQueue = existing;
		}
		return blockingQueue;
	}

	final static class ReleasingMessageHandler extends AbstractMessageHandler {

		private CorrelationStrategy correlationStrategy;

		private ConcurrentHashMap<Object, BlockingQueue<Message<?>>> suspensions;


		public void setCorrelationStrategy(CorrelationStrategy correlationStrategy) {
			this.correlationStrategy = correlationStrategy;
		}

		public void setSuspensions(ConcurrentHashMap<Object, BlockingQueue<Message<?>>> suspensions) {
			this.suspensions = suspensions;
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			Object key = this.correlationStrategy.getCorrelationKey(message);
			if (key == null) {
				throw new MessagingException(message, "Correlation Strategy returned null");
			}
			BlockingQueue<Message<?>> blockingQueue = createOrObtainQueue(this.suspensions, key);
			synchronized(blockingQueue) {
				if (!blockingQueue.offer(message)) {
					this.logger.error("Release message arrived too late: " + message
							+ " pending message: " + blockingQueue.poll());
					this.suspensions.remove(key);
				}
			}
		}

	}

}
