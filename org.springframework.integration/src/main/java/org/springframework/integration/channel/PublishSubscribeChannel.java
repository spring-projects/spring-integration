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

/**
 * A channel that sends Messages to each of its subscribers. 
 * 
 * @author Mark Fisher
 */
public class PublishSubscribeChannel extends AbstractSubscribableChannel<BroadcastingDispatcher> {

	/**
	 * Create a PublishSubscribeChannel that will use a {@link TaskExecutor}
	 * to publish its Messages. 
	 */
	public PublishSubscribeChannel(TaskExecutor taskExecutor) {
		super(new BroadcastingDispatcher());
		if (taskExecutor != null) {
			this.getDispatcher().setTaskExecutor(taskExecutor);
		}
	}

	public PublishSubscribeChannel() {
		this(null);
	}


	public void setApplySequence(boolean applySequence) {
		this.getDispatcher().setApplySequence(applySequence);
	}

}
