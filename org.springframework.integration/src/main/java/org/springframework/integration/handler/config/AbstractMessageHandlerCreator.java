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

package org.springframework.integration.handler.config;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.MessageHandler;

/**
 * Base class for handler creators that generate a {@link MessageHandler}
 * adapter for the provided object and method.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessageHandlerCreator implements MessageHandlerCreator {

	public final MessageHandler createHandler(Object object, Method method, Map<String, ?> attributes) {
		MessageHandler handler = this.doCreateHandler(object, method, attributes);
		if (attributes != null && handler instanceof AbstractMessageHandler) {
			Object order = attributes.get("order");
			if (order != null && order instanceof Integer) {
				((AbstractMessageHandler) handler).setOrder(((Integer) order).intValue());
			}
		}
		if (handler instanceof InitializingBean) {
			try {
				((InitializingBean) handler).afterPropertiesSet();
			}
			catch (Exception e) {
				throw new ConfigurationException("failed to initialize handler", e);
			}
		}
		return handler;
	}

	protected abstract MessageHandler doCreateHandler(Object object, Method method, Map<String, ?> attributes);

}
