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

import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;

/**
 * Base interface for any component that is capable of sending
 * messages to a {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public interface MessageProducer {

	/**
	 * Specify the {@link MessageChannel} to which produced Messages should be sent.
	 * @param outputChannel The output channel.
	 */
	void setOutputChannel(MessageChannel outputChannel);

	/**
	 * Specify the bean name of the {@link MessageChannel} to which produced Messages should be sent.
	 * @param outputChannel The output channel bean name.
	 * @since 5.1.2
	 */
	default void setOutputChannelName(String outputChannel) {
		throw new UnsupportedOperationException("This MessageProducer does not support setting the channel by name.");
	}

	/**
	 * Return the output channel.
	 * @return the channel.
	 * @since 4.3
	 */
	@Nullable
	MessageChannel getOutputChannel();

}
