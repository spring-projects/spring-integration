/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.integration.endpoint.MessageHandlerAdapter;
import org.springframework.integration.handler.MessageHandler;

/**
 * Default implementation of the handler creator strategy that creates a
 * {@link MessageHandlerAdapter} for the provided object and method. This
 * version does not even consider the annotation itself. It does however
 * respect an {@link Order} annotation if present.
 * 
 * @author Mark Fisher
 */
public class DefaultAnnotationHandlerCreator implements AnnotationHandlerCreator {

	public MessageHandler createHandler(Object object, Method method, Annotation annotation) {
		MessageHandlerAdapter<Object> adapter = new MessageHandlerAdapter<Object>();
		adapter.setObject(object);
		adapter.setMethod(method.getName());
		Order orderAnnotation = (Order) AnnotationUtils.getAnnotation(method, Order.class);
		if (orderAnnotation != null) {
			adapter.setOrder(orderAnnotation.value());
		}
		adapter.afterPropertiesSet();
		return adapter;
	}

}
