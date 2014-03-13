/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.expression.MethodFilter;
import org.springframework.integration.expression.ControlBusMethodFilter;
import org.springframework.integration.handler.ExpressionCommandMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageHandler;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a SpEL expression.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ExpressionControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	private static final MethodFilter methodFilter = new ControlBusMethodFilter();

	private volatile Long sendTimeout;


	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	protected MessageHandler createHandler() {
		ExpressionCommandMessageProcessor processor =
				new ExpressionCommandMessageProcessor(methodFilter, this.getBeanFactory());
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}


}
