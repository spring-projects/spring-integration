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

import org.springframework.integration.handler.DefaultMessageHandler;
import org.springframework.integration.handler.MessageHandler;

/**
 * Default implementation of the handler creator strategy that creates a
 * {@link DefaultMessageHandler} for the provided object and method.
 * 
 * @author Mark Fisher
 */
public class DefaultMessageHandlerCreator extends AbstractMessageHandlerCreator {

	public MessageHandler doCreateHandler(Object object, Method method, Map<String, ?> attributes) {
		DefaultMessageHandler handler = new DefaultMessageHandler();
		handler.setObject(object);
		handler.setMethod(method);
		handler.afterPropertiesSet();
		return handler;
	}

}
