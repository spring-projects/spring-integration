/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.concurrent.Executors;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.dispatcher.AbstractUnicastDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.dispatcher.RoundRobinDispatcher;
import org.springframework.integration.message.MessageHandler;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.util.Assert;

/**
 * An implementation of {@link MessageChannel} that delegates to an instance of
 * {@link AbstractUnicastDispatcher} and wraps all send invocations within a
 * {@link TaskExecutor}.
 * 
 * @author Mark Fisher
 * @since 1.0.3
 */
public class ExecutorChannel extends AbstractSubscribableChannel {

	private final ExecutorDecoratingDispatcher dispatcher;


	public ExecutorChannel() {
		this(null, null);
	}

	public ExecutorChannel(TaskExecutor taskExecutor) {
		this(null, taskExecutor);
	}

	public ExecutorChannel(AbstractUnicastDispatcher dispatcher) {
		this(dispatcher, null);
	}

	public ExecutorChannel(AbstractUnicastDispatcher dispatcher, TaskExecutor taskExecutor) {
		if (dispatcher == null) {
			dispatcher = new RoundRobinDispatcher();
		}
		this.dispatcher = new ExecutorDecoratingDispatcher(dispatcher, taskExecutor);
	}


	@Override
	protected MessageDispatcher getDispatcher() {
		return this.dispatcher;
	}


	private static class ExecutorDecoratingDispatcher implements MessageDispatcher {

		private final AbstractUnicastDispatcher targetDispatcher;

		private final TaskExecutor taskExecutor;


		ExecutorDecoratingDispatcher(AbstractUnicastDispatcher dispatcher, TaskExecutor taskExecutor) {
			Assert.notNull(dispatcher, "dispatcher must not be null");
			this.targetDispatcher = dispatcher;
			this.taskExecutor = taskExecutor != null ? taskExecutor
					: new ConcurrentTaskExecutor(Executors.newSingleThreadExecutor());
		}


		public boolean addHandler(MessageHandler handler) {
			return this.targetDispatcher.addHandler(handler);
		}

		public boolean removeHandler(MessageHandler handler) {
			return this.targetDispatcher.removeHandler(handler);
		}

		public final boolean dispatch(final Message<?> message) {
			this.taskExecutor.execute(new Runnable() {
				public void run() {
					targetDispatcher.dispatch(message);
				}
			});
			return true;
		}
	}

}
