/*
 * Copyright 2024-present the original author or authors.
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

package org.springframework.integration.config;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.handler.ControlBusMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageHandler;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message with a Control Bus command.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 */
public class ControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	@Nullable
	private Long sendTimeout;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	protected MessageHandler createHandler() {
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new ControlBusMessageProcessor());
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}

}
