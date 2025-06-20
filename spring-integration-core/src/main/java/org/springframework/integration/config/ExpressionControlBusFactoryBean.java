/*
 * Copyright 2002-present the original author or authors.
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

import org.springframework.expression.MethodFilter;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageHandler;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a SpEL expression.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated in favor of {@link ControlBusFactoryBean}
 */
@Deprecated(since = "6.4", forRemoval = true)
public class ExpressionControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	@SuppressWarnings("removal")
	private static final MethodFilter METHOD_FILTER = new org.springframework.integration.expression.ControlBusMethodFilter();

	private Long sendTimeout;

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@SuppressWarnings("removal")
	@Override
	protected MessageHandler createHandler() {
		org.springframework.integration.handler.ExpressionCommandMessageProcessor processor =
				new org.springframework.integration.handler.ExpressionCommandMessageProcessor(METHOD_FILTER, getBeanFactory());
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}

}
