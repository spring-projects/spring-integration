/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.core;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class AsyncMessagingTemplate extends MessagingTemplate implements AsyncMessagingOperations {

	private static final String UNCHECKED = "unchecked";

	private volatile AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();

	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "executor must not be null");
		this.executor = (executor instanceof AsyncTaskExecutor) ?
				(AsyncTaskExecutor) executor : new TaskExecutorAdapter(executor);
	}

	@Override
	public Future<?> asyncSend(final Message<?> message) {
		return this.executor.submit(() -> send(message));
	}

	@Override
	public Future<?> asyncSend(final MessageChannel channel, final Message<?> message) {
		return this.executor.submit(() -> send(channel, message));
	}

	@Override
	public Future<?> asyncSend(final String channelName, final Message<?> message) {
		return this.executor.submit(() -> send(channelName, message));
	}

	@Override
	public Future<?> asyncConvertAndSend(final Object object) {
		return this.executor.submit(() -> convertAndSend(object));
	}

	@Override
	public Future<?> asyncConvertAndSend(final MessageChannel channel, final Object object) {
		return this.executor.submit(() -> convertAndSend(channel, object));
	}

	@Override
	public Future<?> asyncConvertAndSend(final String channelName, final Object object) {
		return this.executor.submit(() -> convertAndSend(channelName, object));
	}

	@Override
	public Future<Message<?>> asyncReceive() {
		return this.executor.submit(() -> receive());
	}

	@Override
	public Future<Message<?>> asyncReceive(final PollableChannel channel) {
		return this.executor.submit(() -> receive(channel));
	}

	@Override
	public Future<Message<?>> asyncReceive(final String channelName) {
		return this.executor.submit(() -> receive(channelName));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncReceiveAndConvert() {
		return this.executor.submit(() -> (R) receiveAndConvert(Object.class));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncReceiveAndConvert(final PollableChannel channel) {
		return this.executor.submit(() -> (R) receiveAndConvert(channel, Object.class));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncReceiveAndConvert(final String channelName) {
		return this.executor.submit(() -> (R) receiveAndConvert(channelName, Object.class));
	}

	@Override
	public Future<Message<?>> asyncSendAndReceive(final Message<?> requestMessage) {
		return this.executor.submit(() -> sendAndReceive(requestMessage));
	}

	@Override
	public Future<Message<?>> asyncSendAndReceive(final MessageChannel channel, final Message<?> requestMessage) {
		return this.executor.submit(() -> sendAndReceive(channel, requestMessage));
	}

	@Override
	public Future<Message<?>> asyncSendAndReceive(final String channelName, final Message<?> requestMessage) {
		return this.executor.submit(() -> sendAndReceive(channelName, requestMessage));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncConvertSendAndReceive(final Object request) {
		return this.executor.submit(() -> (R) convertSendAndReceive(request, Object.class));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncConvertSendAndReceive(final MessageChannel channel, final Object request) {
		return this.executor.submit(() -> (R) convertSendAndReceive(channel, request, Object.class));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncConvertSendAndReceive(final String channelName, final Object request) {
		return this.executor.submit(() -> (R) convertSendAndReceive(channelName, request, Object.class));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncConvertSendAndReceive(final Object request,
			final MessagePostProcessor requestPostProcessor) {
		return this.executor.submit(() -> (R) convertSendAndReceive(request, Object.class, requestPostProcessor));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncConvertSendAndReceive(final MessageChannel channel, final Object request,
			final MessagePostProcessor requestPostProcessor) {
		return this.executor
				.submit(() -> (R) convertSendAndReceive(channel, request, Object.class, requestPostProcessor));
	}

	@Override
	@SuppressWarnings(UNCHECKED)
	public <R> Future<R> asyncConvertSendAndReceive(final String channelName, final Object request,
			final MessagePostProcessor requestPostProcessor) {
		return this.executor
				.submit(() -> (R) convertSendAndReceive(channelName, request, Object.class, requestPostProcessor));
	}

}
