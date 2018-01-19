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

package org.springframework.integration.micrometer;

import org.springframework.integration.support.management.AbstractMessageChannelMetrics;
import org.springframework.integration.support.management.AbstractMessageHandlerMetrics;
import org.springframework.integration.support.management.DefaultMessageChannelMetrics;
import org.springframework.integration.support.management.DefaultMessageHandlerMetrics;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.support.management.MessageSourceMetricsConfigurer;
import org.springframework.integration.support.management.MetricsFactory;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer implementation.
 *
 * @author Gary Russell
 * @since 5.0.1
 *
 */
public class MicrometerMetricsFactory implements MetricsFactory, MessageSourceMetricsConfigurer {

	private final MeterRegistry meterRegistry;

	public MicrometerMetricsFactory(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public AbstractMessageChannelMetrics createChannelMetrics(String name) {
		return new DefaultMessageChannelMetrics(name, new TimerImpl(this.meterRegistry.timer(name + ".timer")),
				new CounterImpl(this.meterRegistry.counter(name + ".counter")));
	}

	@Override
	public AbstractMessageHandlerMetrics createHandlerMetrics(String name) {
		return new DefaultMessageHandlerMetrics(name, new TimerImpl(this.meterRegistry.timer(name + ".timer")),
				new CounterImpl(this.meterRegistry.counter(name + ".counter")));
	}

	@Override
	public void configure(MessageSourceMetrics metrics, String name) {
		metrics.setCounterFacade(new CounterImpl(this.meterRegistry.counter(name)));
	}

}
