/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.support.management.micrometer;

import java.util.function.Function;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.management.AbstractMessageChannelMetrics;
import org.springframework.integration.support.management.AbstractMessageHandlerMetrics;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.integration.support.management.DefaultMessageHandlerMetrics;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.support.management.MessageSourceMetricsConfigurer;
import org.springframework.integration.support.management.MetricsFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer implementation of a {@link MetricsFactory}. Configures the resulting
 * channel, and handler metrics to use Micrometer metrics instead of the legacy Spring
 * Integration metrics. Also implements {@link MessageSourceMetricsConfigurer}, which is
 * used to inject a counter into all message source beans that implement
 * {@link MessageSourceMetrics}.
 *
 * @author Gary Russell
 *
 * @since 5.0.2
 *
 * @see org.springframework.integration.support.management.IntegrationManagementConfigurer
 */
public class MicrometerMetricsFactory implements MetricsFactory, MessageSourceMetricsConfigurer,
		ApplicationContextAware, SmartInitializingSingleton {

	private static final Function<String, String[]> NO_TAGS = n -> new String[0];

	private final MeterRegistry meterRegistry;

	private ApplicationContext applicationContext;

	private Function<String, String> timerNameProvider = n -> n + ".timer";

	private Function<String, String> counterNameProvider = n -> n + ".counter";

	private Function<String, String> errorCounterNameProvider = n -> n + ".errorCounter";

	private Function<String, String> receiveCounterNameProvider = n -> n + ".receive.counter";

	private Function<String, String> receiveErrorCounterNameProvider = n -> n + ".receive.errorCounter";

	private Function<String, String[]> timerTagProvider = NO_TAGS;

	private Function<String, String[]> counterTagProvider = NO_TAGS;

	private Function<String, String[]> errorCounterTagProvider = NO_TAGS;

	private Function<String, String[]> receiveCounterTagProvider = NO_TAGS;

	private Function<String, String[]> receiveErrorCounterTagProvider = NO_TAGS;

	private Function<String, String[]> componentCountTagProvider = NO_TAGS;

	/**
	 * Construct an instance with the provided {@link MeterRegistry}.
	 * @param meterRegistry the registry.
	 */
	public MicrometerMetricsFactory(MeterRegistry meterRegistry) {
		Assert.notNull(meterRegistry, "'meterRegistry' cannot be null");
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Assert.notNull(this.applicationContext, "An application context is required");
		registerComponentGauges();
	}

	/**
	 * Provide a function to generate a timer name for the bean name.
	 * Default: "beanName.timer".
	 * @param timerNameProvider the timerNameProvider to set
	 */
	public void setTimerNameProvider(Function<String, String> timerNameProvider) {
		Assert.notNull(timerNameProvider, "'timerNameProvider' cannot be null");
		this.timerNameProvider = timerNameProvider;
	}

	/**
	 * Provide a function to generate a counter name for the bean name.
	 * Default: "beanName.counter".
	 * @param counterNameProvider the counterNameProvider to set
	 */
	public void setCounterNameProvider(Function<String, String> counterNameProvider) {
		Assert.notNull(counterNameProvider, "'counterNameProvider' cannot be null");
		this.counterNameProvider = counterNameProvider;
	}

	/**
	 * Provide a function to generate an error counter name for the bean name.
	 * Default: "beanName.errorCounter".
	 * @param errorCounterNameProvider the counterNameProvider to set
	 */
	public void setErrorCounterNameProvider(Function<String, String> errorCounterNameProvider) {
		Assert.notNull(errorCounterNameProvider, "'errorCounterNameProvider' cannot be null");
		this.errorCounterNameProvider = errorCounterNameProvider;
	}

	/**
	 * Provide a function to generate a receive counter name for the bean name.
	 * Default: "beanName.counter".
	 * @param counterNameProvider the counterNameProvider to set
	 */
	public void setReceiveCounterNameProvider(Function<String, String> counterNameProvider) {
		Assert.notNull(counterNameProvider, "'counterNameProvider' cannot be null");
		this.receiveCounterNameProvider = counterNameProvider;
	}

	/**
	 * Provide a function to generate a receive error counter name for the bean name.
	 * Default: "beanName.errorCounter".
	 * @param errorCounterNameProvider the counterNameProvider to set
	 */
	public void setReceiveErrorCounterNameProvider(Function<String, String> errorCounterNameProvider) {
		Assert.notNull(errorCounterNameProvider, "'errorCounterNameProvider' cannot be null");
		this.receiveErrorCounterNameProvider = errorCounterNameProvider;
	}

	/**
	 * Provide a function to generate timer tags for the bean name.
	 * Default: no tags.
	 * @param timerTagProvider the timerTagProvider to set
	 */
	public void setTimerTagProvider(Function<String, String[]> timerTagProvider) {
		Assert.notNull(timerTagProvider, "'timerTagProvider' cannot be null");
		this.timerTagProvider = timerTagProvider;
	}

	/**
	 * Provide a function to generate counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
		this.counterTagProvider = counterTagProvider;
	}

	/**
	 * Provide a function to generate error counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setErrorCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
		this.errorCounterTagProvider = counterTagProvider;
	}

	/**
	 * Provide a function to generate receive counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setReceiveCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
		this.receiveCounterTagProvider = counterTagProvider;
	}

	/**
	 * Provide a function to generate receive error counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setReceiveErrorCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
		this.receiveErrorCounterTagProvider = counterTagProvider;
	}

	/**
	 * Provide a function to generate tags for component (channels, handlers, sources) gauges.
	 * Default: no tags.
	 * @param componentCountTagProvider the componentCountTagProvider to set
	 */
	public void setComponentCountTagProvider(Function<String, String[]> componentCountTagProvider) {
		this.componentCountTagProvider = componentCountTagProvider;
	}

	@Override
	public AbstractMessageChannelMetrics createChannelMetrics(String name) {
		return new DefaultMessageChannelMetrics(name,
				this.meterRegistry.timer(this.timerNameProvider.apply(name), this.timerTagProvider.apply(name)),
				this.meterRegistry.counter(this.errorCounterNameProvider.apply(name),
						this.errorCounterTagProvider.apply(name)), null, null);
	}

	@Override
	public AbstractMessageChannelMetrics createPollableChannelMetrics(String name) {
		return new DefaultMessageChannelMetrics(name,
				this.meterRegistry.timer(this.timerNameProvider.apply(name), this.timerTagProvider.apply(name)),
				this.meterRegistry.counter(this.errorCounterNameProvider.apply(name),
						this.errorCounterTagProvider.apply(name)),
				this.meterRegistry.counter(this.receiveCounterNameProvider.apply(name),
						this.receiveCounterTagProvider.apply(name)),
				this.meterRegistry.counter(this.receiveErrorCounterNameProvider.apply(name),
						this.receiveErrorCounterTagProvider.apply(name)));
	}

	@Override
	public AbstractMessageHandlerMetrics createHandlerMetrics(String name) {
		return new DefaultMessageHandlerMetrics(name,
				this.meterRegistry.timer(this.timerNameProvider.apply(name), this.timerTagProvider.apply(name)),
				this.meterRegistry.counter(this.errorCounterNameProvider.apply(name),
						this.errorCounterTagProvider.apply(name)));
	}

	@Override
	public void configure(MessageSourceMetrics metrics, String name) {
		metrics.setCounter(this.meterRegistry.counter(this.counterNameProvider.apply(name),
				this.counterTagProvider.apply(name)));
	}

	private void registerComponentGauges() {
		Gauge.Builder<?> builder = Gauge.builder("spring.integration.channels", this,
				(c) -> this.applicationContext.getBeansOfType(MessageChannel.class).size());
		builder.tags(this.componentCountTagProvider.apply("channels"))
				.description("The Number of Message Channels")
				.register(this.meterRegistry);

		builder = Gauge.builder("spring.integration.handlers", this,
				(c) -> this.applicationContext.getBeansOfType(MessageHandler.class).size());
		builder.tags(this.componentCountTagProvider.apply("handlers"))
				.description("The Number of Message Handlers")
				.register(this.meterRegistry);

		builder = Gauge.builder("spring.integration.sources", this,
				(c) -> this.applicationContext.getBeansOfType(MessageSource.class).size());
		builder.tags(this.componentCountTagProvider.apply("sources"))
				.description("The number of Message Sources")
				.register(this.meterRegistry);
	}

}
