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

package org.springframework.integration.message;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.util.Assert;

/**
 * An asynchronous version of the {@link MessageExchangeTemplate}.
 * 
 * @author Mark Fisher
 */
public class AsyncMessageExchangeTemplate extends MessageExchangeTemplate {

	private final TaskExecutor taskExecutor;


	public AsyncMessageExchangeTemplate(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "TaskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}


	/**
	 * Send the provided message to the given channel. Note that the actual
	 * sending occurs asynchronously, so this method will always return
	 * <code>true</code> unless an exception is thrown by the executor.
	 */
	@Override
	public boolean send(final Message<?> message, final MessageChannel channel) {
		this.taskExecutor.execute(new Runnable() {
			public void run() {
				AsyncMessageExchangeTemplate.super.send(message, channel);
			}
		});
		return true;
	}

	/**
	 * Send the provided message to the given channel and receive the
	 * result as an {@link AsyncMessage}.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Message<?> sendAndReceive(final Message<?> request, final MessageChannel channel) {
		FutureTask<Message<?>> task = new FutureTask<Message<?>>(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return AsyncMessageExchangeTemplate.super.sendAndReceive(request, channel);
			}
		});
		this.taskExecutor.execute(task);
		return new AsyncMessage(task);
	}

	/**
	 * Receive an {@link AsyncMessage} from the provided source.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Message<?> receive(final PollableSource<?> source) {
		FutureTask<Message<?>> task = new FutureTask<Message<?>>(new Callable<Message<?>>() {
			public Message<?> call() throws Exception {
				return AsyncMessageExchangeTemplate.super.receive(source);
			}
		});
		this.taskExecutor.execute(task);
		return new AsyncMessage(task);
	}

	/**
	 * Receive a Message from the provided source and if not <code>null</code>,
	 * send it to the given channel. Note that the receive and send operations
	 * occur asynchronously, so this method will always return <code>true</code>
	 * unless an exception is thrown by the executor.
	 */
	@Override
	public boolean receiveAndForward(final PollableSource<?> source, final MessageChannel channel) {
		this.taskExecutor.execute(new Runnable() {
			public void run() {
				AsyncMessageExchangeTemplate.super.receiveAndForward(source, channel);
			}
		});
		return true;
	}

}
