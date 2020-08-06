/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.expression.ExpressionEvalMap;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.IntegrationInboundManagement;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.metrics.CounterFacade;
import org.springframework.integration.support.management.metrics.MeterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * Abstract message source.
 *
 * @param <T> The payload type.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@IntegrationManagedResource
public abstract class AbstractMessageSource<T> extends AbstractExpressionEvaluator
		implements MessageSource<T>, IntegrationInboundManagement, NamedComponent, BeanNameAware {

	private final AtomicLong messageCount = new AtomicLong();

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	private final Set<MeterFacade> meters = ConcurrentHashMap.newKeySet();

	private Map<String, Expression> headerExpressions;

	private String beanName;

	private String managedType;

	private String managedName;

	private boolean loggingEnabled = true;

	private MetricsCaptor metricsCaptor;

	private CounterFacade receiveCounter;

	public void setHeaderExpressions(@Nullable Map<String, Expression> headerExpressions) {
		if (!CollectionUtils.isEmpty(headerExpressions)) {
			this.headerExpressions = new HashMap<>(headerExpressions);
		}
	}

	@Override
	public void registerMetricsCaptor(MetricsCaptor metricsCaptorToSet) {
		this.metricsCaptor = metricsCaptorToSet;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	@Override
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setManagedType(String managedType) {
		this.managedType = managedType;
	}

	@Override
	public String getManagedType() {
		return this.managedType;
	}

	@Override
	public void setManagedName(String managedName) {
		this.managedName = managedName;
	}

	@Override
	public String getManagedName() {
		return this.managedName;
	}

	@Override
	public String getComponentName() {
		return this.beanName;
	}

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	@Override
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
		this.managementOverrides.loggingConfigured = true;
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.managementOverrides;
	}

	@Override
	public final Message<T> receive() {
		try {
			return buildMessage(doReceive());
		}
		catch (RuntimeException ex) {
			if (this.metricsCaptor != null) {
				createCounter(false, ex.getClass().getSimpleName()).increment();
			}
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	protected Message<T> buildMessage(Object result) {
		if (result == null) {
			return null;
		}
		Message<?> message;
		Map<String, Object> headers = evaluateHeaders();
		if (result instanceof AbstractIntegrationMessageBuilder<?>) {
			if (!CollectionUtils.isEmpty(headers)) {
				((AbstractIntegrationMessageBuilder<?>) result).copyHeaders(headers);
			}
			message = ((AbstractIntegrationMessageBuilder<?>) result).build();
		}
		else if (result instanceof Message<?>) {
			message = (Message<?>) result;
			if (!CollectionUtils.isEmpty(headers)) {
				// create a new Message from this one in order to apply headers
				message =
						getMessageBuilderFactory()
								.fromMessage(message)
								.copyHeaders(headers)
								.build();
			}
		}
		else {
			message =
					getMessageBuilderFactory()
							.withPayload(result)
							.copyHeaders(headers)
							.build();
		}
		if (this.metricsCaptor != null) {
			incrementReceiveCounter();
		}
		this.messageCount.incrementAndGet();
		return (Message<T>) message;
	}

	private void incrementReceiveCounter() {
		if (this.receiveCounter == null) {
			this.receiveCounter = createCounter(true, "none");
		}
		this.receiveCounter.increment();
	}

	private CounterFacade createCounter(boolean success, String exception) {
		CounterFacade counter = this.metricsCaptor.counterBuilder(RECEIVE_COUNTER_NAME)
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("type", "source")
				.tag("result", success ? "success" : "failure")
				.tag("exception", exception)
				.description("Messages received")
				.build();
		this.meters.add(counter);
		return counter;
	}

	@Nullable
	private Map<String, Object> evaluateHeaders() {
		return CollectionUtils.isEmpty(this.headerExpressions)
				? null
				: ExpressionEvalMap.from(this.headerExpressions)
						.usingEvaluationContext(getEvaluationContext())
						.build();
	}

	/**
	 * Subclasses must implement this method. Typically the returned value will be the {@code payload} of
	 * type T, but the returned value may also be a {@link Message} instance whose payload is of type T;
	 * also can be {@link AbstractIntegrationMessageBuilder} which is used for additional headers population.
	 * @return The value returned.
	 */
	@Nullable
	protected abstract Object doReceive();

	@Override
	public void destroy() {
		this.meters.forEach(MeterFacade::remove);
		this.meters.clear();
	}

}
