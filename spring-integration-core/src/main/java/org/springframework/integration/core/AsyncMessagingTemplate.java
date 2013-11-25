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

package org.springframework.integration.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class AsyncMessagingTemplate extends MessagingTemplate implements AsyncMessagingOperations {

	private volatile AsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();


	public void setExecutor(Executor executor) {
		Assert.notNull(executor, "executor must not be null");
		this.executor = (executor instanceof AsyncTaskExecutor) ?
				(AsyncTaskExecutor) executor : new TaskExecutorAdapter(executor);
	}

	public Future<?> asyncSend(final Message<?> message) {
		return this.executor.submit(new Runnable() {
			public void run() {
				send(message);
			}
		});
	}

	public Future<?> asyncSend(final MessageChannel channel, final Message<?> message) {
		return this.executor.submit(new Runnable() {
			public void run() {
				send(channel, message);
			}
		});
	}

	public Future<?> asyncSend(final String channelName, final Message<?> message) {
		return this.executor.submit(new Runnable() {
			public void run() {
				send(channelName, message);
			}
		});
	}

	public Future<?> asyncConvertAndSend(final Object object) {
		return this.executor.submit(new Runnable() {
			public void run() {
				convertAndSend(object);
			}
		});
	}

	public Future<?> asyncConvertAndSend(final MessageChannel channel, final Object object) {
		return this.executor.submit(new Runnable() {
			public void run() {
				convertAndSend(channel, object);
			}
		});
	}

	public Future<?> asyncConvertAndSend(final String channelName, final Object object) {
		return this.executor.submit(new Runnable() {
			public void run() {
				convertAndSend(channelName, object);
			}
		});
	}

	public Future<Message<?>> asyncReceive() {
		return this.executor.submit(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return receive();
			}
		});
	}

	public Future<Message<?>> asyncReceive(final PollableChannel channel) {
		return this.executor.submit(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return receive(channel);
			}
		});
	}

	public Future<Message<?>> asyncReceive(final String channelName) {
		return this.executor.submit(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return receive(channelName);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncReceiveAndConvert() {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) receiveAndConvert(null);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncReceiveAndConvert(final PollableChannel channel) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) receiveAndConvert(channel, null);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncReceiveAndConvert(final String channelName) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) receiveAndConvert(channelName, null);
			}
		});
	}

	public Future<Message<?>> asyncSendAndReceive(final Message<?> requestMessage) {
		return this.executor.submit(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return sendAndReceive(requestMessage);
			}
		});
	}

	public Future<Message<?>> asyncSendAndReceive(final MessageChannel channel, final Message<?> requestMessage) {
		return this.executor.submit(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return sendAndReceive(channel, requestMessage);
			}
		});
	}

	public Future<Message<?>> asyncSendAndReceive(final String channelName, final Message<?> requestMessage) {
		return this.executor.submit(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return sendAndReceive(channelName, requestMessage);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncConvertSendAndReceive(final Object request) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) convertSendAndReceive(request, null);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncConvertSendAndReceive(final MessageChannel channel, final Object request) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) convertSendAndReceive(channel, request, null);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncConvertSendAndReceive(final String channelName, final Object request) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) convertSendAndReceive(channelName, request, null);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncConvertSendAndReceive(final Object request, final MessagePostProcessor requestPostProcessor) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) convertSendAndReceive(request, null, requestPostProcessor);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncConvertSendAndReceive(final MessageChannel channel, final Object request, final MessagePostProcessor requestPostProcessor) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) convertSendAndReceive(channel, request, null, requestPostProcessor);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <R> Future<R> asyncConvertSendAndReceive(final String channelName, final Object request, final MessagePostProcessor requestPostProcessor) {
		return this.executor.submit(new Callable<R>() {
			public R call() throws Exception {
				return (R) convertSendAndReceive(channelName, request, null, requestPostProcessor);
			}
		});
	}

}
