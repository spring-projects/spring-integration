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

package org.springframework.integration.channel;

import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.SubscribableSource;

/**
 * @author Mark Fisher
 */
public class PublishSubscribeChannel extends AbstractMessageChannel implements SubscribableSource {

	private final BroadcastingDispatcher dispatcher = new BroadcastingDispatcher();


	/**
	 * Create a PublishSubscribeChannel that will use a {@link TaskExecutor}
	 * to publish its Messages. 
	 */
	public PublishSubscribeChannel(TaskExecutor taskExecutor) {
		if (taskExecutor != null) {
			this.dispatcher.setTaskExecutor(taskExecutor);
		}
	}

	public PublishSubscribeChannel() {
	}


	public void setApplySequence(boolean applySequence) {
		this.dispatcher.setApplySequence(applySequence);
	}

	public boolean subscribe(MessageTarget target) {
		return this.dispatcher.subscribe(target);
	}

	public boolean unsubscribe(MessageTarget target) {
		return this.dispatcher.unsubscribe(target);
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		return this.dispatcher.send(message);
	}

}
