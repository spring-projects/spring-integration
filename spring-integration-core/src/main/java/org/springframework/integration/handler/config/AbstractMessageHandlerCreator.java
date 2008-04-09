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
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.handler.AbstractMessageHandlerAdapter;
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
		if (handler instanceof AbstractMessageHandlerAdapter<?>) {
			AbstractMessageHandlerAdapter adapter = ((AbstractMessageHandlerAdapter) handler);
			adapter.setObject(object);
			adapter.setMethodName(method.getName());
			Order orderAnnotation = (Order) AnnotationUtils.getAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				adapter.setOrder(orderAnnotation.value());
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
