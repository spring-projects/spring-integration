/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * Message consumers implement this interface, the message handler within a consumer
 * may or may not emit output messages.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public interface IntegrationConsumer extends NamedComponent {

	/**
	 * Return the input channel.
	 * @return the input channel.
	 */
	MessageChannel getInputChannel();

	/**
	 * Return the output channel (may be null).
	 * @return the output channel.
	 */
	MessageChannel getOutputChannel();

	/**
	 * Return the consumer's handler.
	 * @return the handler.
	 */
	MessageHandler getHandler();

}
