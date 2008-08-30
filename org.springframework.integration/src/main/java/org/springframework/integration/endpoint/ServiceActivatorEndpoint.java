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

package org.springframework.integration.endpoint;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMappingMethodInvoker;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class ServiceActivatorEndpoint extends AbstractInOutEndpoint implements InitializingBean {

	private final MessageMappingMethodInvoker invoker;

	private final MessageHandler handler;


	public ServiceActivatorEndpoint(MessageMappingMethodInvoker invoker) {
		Assert.notNull(invoker, "invoker must not be null");
		this.invoker = invoker;
		this.handler = null;
	}

	public ServiceActivatorEndpoint(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		this.handler = handler;
		this.invoker = null;
	}


	public void afterPropertiesSet() throws Exception {
		if (this.invoker != null) {
			this.invoker.afterPropertiesSet();
		}
	}

	@Override
	protected Object handle(Message<?> message) {
		if (this.invoker != null) {
			return this.invoker.invokeMethod(message);
		}
		return this.handler.handle(message);
	}

}
