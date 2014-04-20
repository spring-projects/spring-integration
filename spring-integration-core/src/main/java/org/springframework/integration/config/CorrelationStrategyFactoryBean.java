/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.config;

import java.lang.reflect.Method;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy;
import org.springframework.integration.config.annotation.MessagingAnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * Convenience factory for XML configuration of a {@link CorrelationStrategy}. Encapsulates the knowledge of the default
 * strategy and search algorithms for POJO and annotated methods.
 *
 * @author Dave Syer
 *
 */
public class CorrelationStrategyFactoryBean implements FactoryBean<CorrelationStrategy> {

	private CorrelationStrategy delegate = new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID);

	/**
	 * Create a factory and set up the delegate which clients of the factory will see as its product.
	 *
	 * @param target the target object (null if default strategy is acceptable)
	 */
	public CorrelationStrategyFactoryBean(Object target) {
		this(target, null);
	}

	/**
	 * Create a factory and set up the delegate which clients of the factory will see as its product.
	 *
	 * @param target the target object (null if default strategy is acceptable)
	 * @param methodName the method name to invoke in the target (null if it can be inferred)
	 */
	public CorrelationStrategyFactoryBean(Object target, String methodName) {
		if (target instanceof CorrelationStrategy && !StringUtils.hasText(methodName)) {
			delegate = (CorrelationStrategy) target;
			return;
		}
		if (target != null) {
			if (StringUtils.hasText(methodName)) {
				delegate = new MethodInvokingCorrelationStrategy(target, methodName);
			}
			else {
				Method method = MessagingAnnotationUtils.findAnnotatedMethod(target,
						org.springframework.integration.annotation.CorrelationStrategy.class);
				if (method != null) {
					delegate = new MethodInvokingCorrelationStrategy(target, method);
				}
			}
		}
	}

	public CorrelationStrategy getObject() throws Exception {
		return delegate;
	}

	public Class<?> getObjectType() {
		return CorrelationStrategy.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
