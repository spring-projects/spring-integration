/*
 * Copyright 2015-2016 the original author or authors.
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

import org.springframework.integration.support.management.AbstractMessageChannelMetrics;
import org.springframework.integration.support.management.AbstractMessageHandlerMetrics;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.integration.support.management.DefaultMessageHandlerMetrics;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.support.management.MessageSourceMetricsConfigurer;
import org.springframework.integration.support.management.MetricsFactory;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer implementation.
 *
 * @author Gary Russell
 * @since 5.0.2
 *
 */
public class MicrometerMetricsFactory implements MetricsFactory, MessageSourceMetricsConfigurer {

	private static final Function<String, String[]> NO_TAGS = n -> new String[0];

	private final MeterRegistry meterRegistry;

	public MicrometerMetricsFactory(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public Function<String, String> timerNameProvider = n -> n + ".timer";

	public Function<String, String> counterNameProvider = n -> n + ".counter";

	public Function<String, String> errorCounterNameProvider = n -> n + ".errorCounter";

	public Function<String, String[]> timerTagProvider = NO_TAGS;

	public Function<String, String[]> counterTagProvider = NO_TAGS;

	public Function<String, String[]> errorCounterTagProvider = NO_TAGS;

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

	@Override
	public AbstractMessageChannelMetrics createChannelMetrics(String name) {
		return new DefaultMessageChannelMetrics(name,
				this.meterRegistry.timer(this.timerNameProvider.apply(name), this.timerTagProvider.apply(name)),
				this.meterRegistry.counter(this.errorCounterNameProvider.apply(name),
						this.errorCounterTagProvider.apply(name)));
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

}
