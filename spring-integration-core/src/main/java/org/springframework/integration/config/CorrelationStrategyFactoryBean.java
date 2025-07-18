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

import java.lang.reflect.Method;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MethodInvokingCorrelationStrategy;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * Convenience factory for XML configuration of a {@link CorrelationStrategy}.
 * Encapsulates the knowledge of the default strategy and search algorithms for POJO and annotated methods.
 *
 * @author Dave Syer
 * @author Artem Bilan
 *
 */
public class CorrelationStrategyFactoryBean implements FactoryBean<CorrelationStrategy>, InitializingBean {

	@SuppressWarnings("NullAway.Init")
	private Object target;

	@Nullable
	private String methodName;

	private CorrelationStrategy strategy =
			new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID);

	public CorrelationStrategyFactoryBean() {
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.target instanceof CorrelationStrategy && !StringUtils.hasText(this.methodName)) {
			this.strategy = (CorrelationStrategy) this.target;
			return;
		}
		if (this.target != null) {
			if (StringUtils.hasText(this.methodName)) {
				this.strategy = new MethodInvokingCorrelationStrategy(this.target, this.methodName);
			}
			else {
				Method method = MessagingAnnotationUtils.findAnnotatedMethod(this.target,
						org.springframework.integration.annotation.CorrelationStrategy.class);
				if (method != null) {
					this.strategy = new MethodInvokingCorrelationStrategy(this.target, method);
				}
			}
		}
	}

	@NonNull
	@Override
	public CorrelationStrategy getObject() {
		return this.strategy;
	}

	@Override
	public Class<?> getObjectType() {
		return CorrelationStrategy.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
