/*
 * Copyright 2015-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel.interceptor;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import io.micrometer.common.lang.Nullable;

import org.springframework.integration.support.MessageDecorator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ExecutorChannelInterceptor;

/**
 * The {@link ExecutorChannelInterceptor} implementation responsible for
 * the {@link Thread} (any?) state propagation from one message flow's thread to another
 * through the {@link MessageChannel}s involved in the flow.
 * <p>
 * The propagation is done from the {@link #preSend(Message, MessageChannel)}
 * implementation using some internal {@link Message} extension which keeps the message
 * to send and the state to propagate.
 * <p>
 * The propagated state context extraction and population is done from the {@link #postReceive}
 * implementation for the {@link org.springframework.messaging.PollableChannel}s, and from
 * the {@link #beforeHandle} for the
 * {@link org.springframework.integration.channel.AbstractExecutorChannel}s and
 * {@link org.springframework.messaging.support.ExecutorSubscribableChannel}s
 * <p>
 * Important. Any further interceptor, which modifies the message to send
 * (e.g. {@code MessageBuilder.withPayload(...)...build()}), may drop the state to propagate.
 * Such kind of interceptors combination should be revised properly.
 * In most cases the interceptors reordering is enough to overcome the issue.
 *
 * @param <S> the propagated state object type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 */
public abstract class ThreadStatePropagationChannelInterceptor<S> implements ExecutorChannelInterceptor {

	@Override
	public final Message<?> preSend(Message<?> message, MessageChannel channel) {
		S threadContext = obtainPropagatingContext(message, channel);
		if (threadContext != null) {
			if (message instanceof MessageWithThreadState messageWithThreadState) {
				messageWithThreadState.stateQueue.add(threadContext);
			}
			else {
				return new MessageWithThreadState(message, threadContext);
			}
		}

		return message;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Message<?> postReceive(Message<?> message, MessageChannel channel) {
		if (message instanceof MessageWithThreadState messageWithThreadState) {
			Object threadContext = messageWithThreadState.stateQueue.poll();
			Message<?> messageToHandle = messageWithThreadState;
			if (messageWithThreadState.stateQueue.isEmpty()) {
				messageToHandle = messageWithThreadState.message;
			}
			populatePropagatedContext((S) threadContext, messageToHandle, channel);
			return messageToHandle;
		}
		return message;
	}

	@Override
	public final Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
		return postReceive(message, channel);
	}

	@Nullable
	protected abstract S obtainPropagatingContext(Message<?> message, MessageChannel channel);

	protected abstract void populatePropagatedContext(@Nullable S state, Message<?> message, MessageChannel channel);

	private static final class MessageWithThreadState implements Message<Object>, MessageDecorator {

		private final Message<Object> message;

		private final Queue<Object> stateQueue;

		MessageWithThreadState(Message<?> message, Object state) {
			this(message, new LinkedBlockingQueue<>());
			this.stateQueue.add(state);
		}

		@SuppressWarnings("unchecked")
		private MessageWithThreadState(Message<?> message, Queue<Object> stateQueue) {
			this.message = (Message<Object>) message;
			this.stateQueue = new LinkedBlockingQueue<>(stateQueue);
		}

		@Override
		public Object getPayload() {
			return this.message.getPayload();
		}

		@Override
		public MessageHeaders getHeaders() {
			return this.message.getHeaders();
		}

		@Override
		public Message<?> decorateMessage(Message<?> message) {
			return new MessageWithThreadState(message, this.stateQueue);
		}

		@Override
		public String toString() {
			return "MessageWithThreadState{" +
					"message=" + this.message +
					", state=" + this.stateQueue +
					'}';
		}

	}

}
