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

package org.springframework.integration.dispatcher;

import java.util.List;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.Subscribable;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public class PublishSubscribeChannel extends AbstractMessageChannel implements Subscribable {

	private final BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();


	public PublishSubscribeChannel() {
	}

	/**
	 * Create a PublishSubscribeChannel that will use a {@link TaskExecutor}
	 * to publish its Messages. 
	 */
	public PublishSubscribeChannel(TaskExecutor taskExecutor) {
		if (taskExecutor != null) {
			this.dispatcher.setTaskExecutor(taskExecutor);
		}
	}


	public boolean subscribe(MessageTarget target) {
		return this.dispatcher.addTarget(target);
	}

	public boolean unsubscribe(MessageTarget target) {
		return this.dispatcher.removeTarget(target);
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		return this.dispatcher.send(message);
	}

	@Override
	protected Message<?> doReceive(long timeout) {
		return null;
	}

	public List<Message<?>> clear() {
		return null;
	}

	public List<Message<?>> purge(MessageSelector selector) {
		return null;
	}

}
