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

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.dispatcher.UnicastingDispatcher;

/**
 * An implementation of {@link MessageChannel} that delegates to an instance of
 * {@link UnicastingDispatcher} and wraps all send invocations within a
 * {@link TaskExecutor}.
 * 
 * @author Mark Fisher
 * @since 1.0.3
 */
public class ExecutorChannel extends AbstractSubscribableChannel {

	private final UnicastingDispatcher dispatcher;


	public ExecutorChannel(TaskExecutor taskExecutor) {
		this.dispatcher = new UnicastingDispatcher(taskExecutor);
	}


	@Override
	protected UnicastingDispatcher getDispatcher() {
		return this.dispatcher;
	}

}
