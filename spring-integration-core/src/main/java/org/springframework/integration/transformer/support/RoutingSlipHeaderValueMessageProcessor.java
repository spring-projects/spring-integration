/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.transformer.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.routingslip.ExpressionEvaluationRoutingSlipRouteStrategy;
import org.springframework.integration.routingslip.RoutingSlipRouteStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The {@code RoutingSlip} {@link HeaderValueMessageProcessor} specific implementation.
 * Accepts the {@code routingSlipPath} array, checks each of them against
 * {@link BeanFactory} on the first {@link #processMessage} invocation.
 * Converts those items, which aren't beans in the application context, to the
 * {@link ExpressionEvaluationRoutingSlipRouteStrategy} and return a {@code singletonMap}
 * with the {@code path} as {@code key} and {@code 0} as initial {@code routingSlipIndex}.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class RoutingSlipHeaderValueMessageProcessor
		extends AbstractHeaderValueMessageProcessor<Map<List<Object>, Integer>>
		implements BeanFactoryAware, IntegrationEvaluationContextAware {

	private final List<String> routingSlipPath;

	private EvaluationContext evaluationContext;

	private volatile Map<List<Object>, Integer> routingSlip;

	private BeanFactory beanFactory;

	public RoutingSlipHeaderValueMessageProcessor(String... routingSlipPath) {
		Assert.notNull(routingSlipPath);
		Assert.noNullElements(routingSlipPath);
		this.routingSlipPath = Arrays.asList(routingSlipPath);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public Map<List<Object>, Integer> processMessage(Message<?> message) {
		if (this.routingSlip == null) {
			synchronized (this) {
				if (this.routingSlip == null) {
					List<Object> routingSlipValues = new ArrayList<Object>(this.routingSlipPath.size());
					for (String path : this.routingSlipPath) {
						if (this.beanFactory.containsBean(path)) {
							Object bean = this.beanFactory.getBean(path);
							Assert.state(bean instanceof MessageChannel || bean instanceof RoutingSlipRouteStrategy,
									"The RoutingSlip can contain only bean names of MessageChannel or " +
											"RoutingSlipRouteStrategy: " + bean);
							routingSlipValues.add(path);
						}
						else {
							ExpressionEvaluationRoutingSlipRouteStrategy strategy = new
									ExpressionEvaluationRoutingSlipRouteStrategy(path);
							strategy.setIntegrationEvaluationContext(this.evaluationContext);
							routingSlipValues.add(strategy);
						}
					}
					this.routingSlip = Collections.singletonMap(Collections.unmodifiableList(routingSlipValues), 0);
				}
			}
		}
		return this.routingSlip;
	}
}
