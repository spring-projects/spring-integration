/*
 * Copyright 2002-2011 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.MethodFilter;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.handler.ExpressionCommandMessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.ReflectionUtils;

/**
 * FactoryBean for creating {@link MessageHandler} instances to handle a message as a SpEL expression.
 * 
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ExpressionControlBusFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<MessageHandler> {

	private volatile Long sendTimeout;

	private final MethodFilter methodFilter = new ControlBusMethodFilter();


	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	protected MessageHandler createHandler() {
		ExpressionCommandMessageProcessor processor = new ExpressionCommandMessageProcessor(this.methodFilter);
		processor.setBeanFactory(this.getBeanFactory());
		ServiceActivatingHandler handler = new ServiceActivatingHandler(processor);
		if (this.sendTimeout != null) {
			handler.setSendTimeout(this.sendTimeout);
		}
		return handler;
	}


	private static class ControlBusMethodFilter implements MethodFilter {

		public List<Method> filter(List<Method> methods) {
			List<Method> supportedMethods = new ArrayList<Method>();
			for (Method method : methods) {
				if (this.accept(method)) {
					supportedMethods.add(method);
				}
			}
			return supportedMethods;
		}

		private boolean accept(Method method) {
			Class<?> declaringClass = method.getDeclaringClass();
			if (Lifecycle.class.isAssignableFrom(declaringClass)
					&& ReflectionUtils.findMethod(Lifecycle.class, method.getName(), method.getParameterTypes()) != null) {
				return true;
			}
			if (CustomizableThreadCreator.class.isAssignableFrom(declaringClass)
					&& (method.getName().startsWith("get")
							|| method.getName().startsWith("set")
							|| method.getName().startsWith("shutdown"))) {
				return true;
			}
			if (this.hasAnnotation(method, ManagedAttribute.class) || this.hasAnnotation(method, ManagedOperation.class)) {
				return true;
			}
			return false;
		}

		private boolean hasAnnotation(Method method, Class<? extends Annotation> annotationType) {
			return AnnotationUtils.findAnnotation(method, annotationType) != null;
		}
	}

}
