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
import org.springframework.integration.support.management.AbstractMessageChannelMetrics;
import org.springframework.integration.support.management.AbstractMessageHandlerMetrics;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.integration.support.management.DefaultMessageHandlerMetrics;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.support.management.MessageSourceMetricsConfigurer;
import org.springframework.integration.support.management.MetricsCaptor;
import org.springframework.integration.support.management.MetricsFactory;
import org.springframework.util.Assert;

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
 * @deprecated - micrometer metrics are now in-built.
 *
 * @see org.springframework.integration.support.management.IntegrationManagementConfigurer
 */
@Deprecated
public class MicrometerMetricsFactory implements MetricsFactory, MessageSourceMetricsConfigurer,
		ApplicationContextAware, SmartInitializingSingleton {

	/**
	 * Construct an instance with the provided {@link MetricsCaptor}.
	 * @param captor the registry.
	 */
	public MicrometerMetricsFactory(MetricsCaptor captor) {
		Assert.notNull(captor, "'meterRegistry' cannot be null");
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
	}

	@Override
	public void afterSingletonsInstantiated() {
	}

	/**
	 * Provide a function to generate a timer name for the bean name.
	 * Default: "beanName.timer".
	 * @param timerNameProvider the timerNameProvider to set
	 */
	public void setTimerNameProvider(Function<String, String> timerNameProvider) {
		Assert.notNull(timerNameProvider, "'timerNameProvider' cannot be null");
	}

	/**
	 * Provide a function to generate a counter name for the bean name.
	 * Default: "beanName.counter".
	 * @param counterNameProvider the counterNameProvider to set
	 */
	public void setCounterNameProvider(Function<String, String> counterNameProvider) {
		Assert.notNull(counterNameProvider, "'counterNameProvider' cannot be null");
	}

	/**
	 * Provide a function to generate an error counter name for the bean name.
	 * Default: "beanName.errorCounter".
	 * @param errorCounterNameProvider the counterNameProvider to set
	 */
	public void setErrorCounterNameProvider(Function<String, String> errorCounterNameProvider) {
		Assert.notNull(errorCounterNameProvider, "'errorCounterNameProvider' cannot be null");
	}

	/**
	 * Provide a function to generate a receive counter name for the bean name.
	 * Default: "beanName.counter".
	 * @param counterNameProvider the counterNameProvider to set
	 */
	public void setReceiveCounterNameProvider(Function<String, String> counterNameProvider) {
		Assert.notNull(counterNameProvider, "'counterNameProvider' cannot be null");
	}

	/**
	 * Provide a function to generate a receive error counter name for the bean name.
	 * Default: "beanName.errorCounter".
	 * @param errorCounterNameProvider the counterNameProvider to set
	 */
	public void setReceiveErrorCounterNameProvider(Function<String, String> errorCounterNameProvider) {
		Assert.notNull(errorCounterNameProvider, "'errorCounterNameProvider' cannot be null");
	}

	/**
	 * Provide a function to generate timer tags for the bean name.
	 * Default: no tags.
	 * @param timerTagProvider the timerTagProvider to set
	 */
	public void setTimerTagProvider(Function<String, String[]> timerTagProvider) {
		Assert.notNull(timerTagProvider, "'timerTagProvider' cannot be null");
	}

	/**
	 * Provide a function to generate counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
	}

	/**
	 * Provide a function to generate error counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setErrorCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
	}

	/**
	 * Provide a function to generate receive counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setReceiveCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
	}

	/**
	 * Provide a function to generate receive error counter tags for the bean name.
	 * Default: no tags.
	 * @param counterTagProvider the counterTagProvider to set
	 */
	public void setReceiveErrorCounterTagProvider(Function<String, String[]> counterTagProvider) {
		Assert.notNull(counterTagProvider, "'counterTagProvider' cannot be null");
	}

	/**
	 * Provide a function to generate tags for component (channels, handlers, sources) gauges.
	 * Default: no tags.
	 * @param componentCountTagProvider the componentCountTagProvider to set
	 */
	public void setComponentCountTagProvider(Function<String, String[]> componentCountTagProvider) {
	}

	@Override
	public AbstractMessageChannelMetrics createChannelMetrics(String name) {
		return new DefaultMessageChannelMetrics(name);
	}

	@Override
	public AbstractMessageChannelMetrics createPollableChannelMetrics(String name) {
		return new DefaultMessageChannelMetrics(name);
	}

	@Override
	public AbstractMessageHandlerMetrics createHandlerMetrics(String name) {
		return new DefaultMessageHandlerMetrics(name);
	}

	@Override
	public void configure(MessageSourceMetrics metrics, String name) {
	}

}
