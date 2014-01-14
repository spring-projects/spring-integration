/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.List;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;

/**
 * Operations available on a channel that has queuing semantics.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface QueueChannelOperations {

	/**
	 * Remove all {@link Message Messages} from this channel.
	 *
	 * @return The messages that were removed.
	 */
	List<Message<?>> clear();

	/**
	 * Remove any {@link Message Messages} that are not accepted by the provided selector.
	 *
	 * @param selector The message selector.
	 * @return The list of messages that were purged.
	 */
	List<Message<?>> purge(MessageSelector selector);

	/**
	 * @return The current number of queued {@link Message Messages} in this channel.
	 */
	int getQueueSize();

	/**
	 * @return The remaining capacity of this channel.
	 */
	int getRemainingCapacity();

}
