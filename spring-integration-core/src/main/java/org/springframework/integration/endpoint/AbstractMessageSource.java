/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.expression.Expression;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.CollectionUtils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@IntegrationManagedResource
public abstract class AbstractMessageSource<T> extends AbstractExpressionEvaluator
		implements MessageSource<T>, MessageSourceMetrics, NamedComponent, BeanNameAware {

	private final AtomicLong messageCount = new AtomicLong();

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	private volatile Map<String, Expression> headerExpressions = Collections.emptyMap();

	private String beanName;

	private String managedType;

	private String managedName;

	private volatile boolean countsEnabled;

	private volatile boolean loggingEnabled = true;

	private MeterRegistry meterRegistry;

	private Counter receiveCounter;

	public void setHeaderExpressions(Map<String, Expression> headerExpressions) {
		this.headerExpressions = (headerExpressions != null)
				? headerExpressions : Collections.emptyMap();
	}

	@Override
	public void registerMeterRegistry(MeterRegistry registry) {
		this.meterRegistry = registry;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
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
	public boolean isCountsEnabled() {
		return this.countsEnabled;
	}

	@Override
	public void setCountsEnabled(boolean countsEnabled) {
		this.countsEnabled = countsEnabled;
		this.managementOverrides.countsConfigured = true;
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
	public void reset() {
		this.messageCount.set(0);
	}

	@Override
	public int getMessageCount() {
		return (int) this.messageCount.get();
	}

	@Override
	public long getMessageCountLong() {
		return this.messageCount.get();
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.managementOverrides;
	}

	@Override
	public final Message<T> receive() {
		return buildMessage(doReceive());
	}

	@SuppressWarnings("unchecked")
	protected Message<T> buildMessage(Object result) {
		Message<T> message = null;
		Map<String, Object> headers = evaluateHeaders();
		if (result instanceof AbstractIntegrationMessageBuilder) {
			if (!CollectionUtils.isEmpty(headers)) {
				((AbstractIntegrationMessageBuilder<T>) result).copyHeaders(headers);
			}
			message = ((AbstractIntegrationMessageBuilder<T>) result).build();
		}
		else if (result instanceof Message<?>) {
			try {
				message = (Message<T>) result;
			}
			catch (Exception e) {
				throw new MessagingException("MessageSource returned unexpected type.", e);
			}
			if (!CollectionUtils.isEmpty(headers)) {
				// create a new Message from this one in order to apply headers
				message = getMessageBuilderFactory()
						.fromMessage(message)
						.copyHeaders(headers)
						.build();
			}
		}
		else if (result != null) {
			T payload;
			try {
				payload = (T) result;
			}
			catch (Exception e) {
				throw new MessagingException("MessageSource returned unexpected type.", e);
			}
			message = getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.build();
		}
		if (this.countsEnabled && message != null) {
			if (this.meterRegistry != null) {
				incrementReceiveCounter();
			}
			this.messageCount.incrementAndGet();
		}
		return message;
	}

	private void incrementReceiveCounter() {
		if (this.receiveCounter == null) {
			this.receiveCounter = Counter.builder(RECEIVE_COUNTER_NAME)
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("type", "source")
				.tag("result", "success")
				.tag("exception", "none")
				.description("Messages received")
				.register(this.meterRegistry);
		}
		this.receiveCounter.increment();
	}

	private Map<String, Object> evaluateHeaders() {
		Map<String, Object> results = new HashMap<>();
		for (Map.Entry<String, Expression> entry : this.headerExpressions.entrySet()) {
			Object headerValue = this.evaluateExpression(entry.getValue());
			if (headerValue != null) {
				results.put(entry.getKey(), headerValue);
			}
		}
		return results;
	}

	/**
	 * Subclasses must implement this method. Typically the returned value will be the {@code payload} of
	 * type T, but the returned value may also be a {@link Message} instance whose payload is of type T;
	 * also can be {@link AbstractIntegrationMessageBuilder} which is used for additional headers population.
	 * @return The value returned.
	 */
	protected abstract Object doReceive();

}
