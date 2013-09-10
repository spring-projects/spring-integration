/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.channel.registry;

import org.springframework.messaging.MessageChannel;

/**
 * A strategy interface used to bind a {@link MessageChannel} to a logical name. The name
 * is intended to identify a logical consumer or producer of messages. This may be a
 * queue, a channel adapter, another message channel, a Spring bean, etc.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @since 3.0
 */
public interface ChannelRegistry {

	/**
	 * Register a message consumer
	 * @param name the logical identity of the message source
	 * @param channel the channel bound as a consumer
	 */
	void inbound(String name, MessageChannel channel);

	/**
	 * Register a message producer
	 * @param name the logical identity of the message target
	 * @param channel the channel bound as a producer
	 */
	void outbound(String name, MessageChannel channel);

	/**
	 * Create a tap on an already registered inbound channel
	 * @param name the registered name
	 * @param channel the channel that will receive messages from the tap
	 */
	void tap(String name, MessageChannel channel);

}
