/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.integration.expression;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 * @deprecated since 4.2 in favor of {@link IntegrationContextUtils#getEvaluationContext}
 * direct usage from the {@code afterPropertiesSet} implementation.
 * Will be removed in the next release.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class IntegrationEvaluationContextAwareBeanPostProcessor
		implements BeanPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

	private final List<IntegrationEvaluationContextAware> evaluationContextAwares =
			new ArrayList<IntegrationEvaluationContextAware>();

	private volatile BeanFactory beanFactory;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof IntegrationEvaluationContextAware) {
			this.evaluationContextAwares.add((IntegrationEvaluationContextAware) bean);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public void afterSingletonsInstantiated() {
		StandardEvaluationContext evaluationContext = IntegrationContextUtils.getEvaluationContext(this.beanFactory);
		for (IntegrationEvaluationContextAware evaluationContextAware : this.evaluationContextAwares) {
			evaluationContextAware.setIntegrationEvaluationContext(evaluationContext);
		}
	}

	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE;
	}

}
