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

import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.support.management.metrics.CounterFacade;
import org.springframework.integration.support.management.metrics.GaugeFacade;
import org.springframework.integration.support.management.metrics.MeterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.management.metrics.SampleFacade;
import org.springframework.integration.support.management.metrics.TimerFacade;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * The Micrometer implementation of {@link MetricsCaptor}.
 *
 * @author Gary Russell
 *
 * @since 5.0.4
 *
 */
public class MicrometerMetricsCaptor implements MetricsCaptor {

	public static final String MICROMETER_CAPTOR_NAME = "integrationMicrometerMetricsCaptor";

	private final MeterRegistry meterRegistry;

	public MicrometerMetricsCaptor(MeterRegistry meterRegistry) {
		Assert.notNull(meterRegistry, "meterRegistry cannot be null");
		this.meterRegistry = meterRegistry;
	}

	@Override
	public TimerBuilder timerBuilder(String name) {
		return new MicroTimerBuilder(this.meterRegistry, name);
	}

	@Override
	public CounterBuilder counterBuilder(String name) {
		return new MicroCounterBuilder(this.meterRegistry, name);
	}

	@Override
	public GaugeBuilder gaugeBuilder(String name, Object obj, ToDoubleFunction<Object> f) {
		return new MicroGaugeBuilder(this.meterRegistry, name, obj, f);
	}

	@Override
	public SampleFacade start() {
		return new MicroSample(Timer.start(this.meterRegistry));
	}

	@Override
	public MeterFacade removeMeter(MeterFacade facade) {
		return facade.remove();
	}

	/**
	 * Add a MicrometerMetricsCaptor to the context if there's a MeterRegistry.
	 * @param applicationContext the application context.
	 * @return the instance.
	 */
	public static MetricsCaptor loadCaptor(ApplicationContext applicationContext) {
		try {
			MeterRegistry registry = applicationContext.getBean(MeterRegistry.class);
			if (applicationContext instanceof GenericApplicationContext
					&& !applicationContext.containsBean(MICROMETER_CAPTOR_NAME)) {
				((GenericApplicationContext) applicationContext).registerBean(MICROMETER_CAPTOR_NAME,
						MicrometerMetricsCaptor.class,
						() -> new MicrometerMetricsCaptor(registry));
				return applicationContext.getBean(MicrometerMetricsCaptor.class);
			}
		}
		catch (NoSuchBeanDefinitionException e) {
			return null;
		}
		return null;
	}

	private class MicroSample implements SampleFacade {

		private final Timer.Sample sample;

		MicroSample(Timer.Sample sample) {
			this.sample = sample;
		}

		@Override
		public void stop(TimerFacade timer) {
			this.sample.stop(((MicroTimer) timer).timer);
		}

	}

	private static class MicroTimerBuilder implements TimerBuilder {

		private final MeterRegistry meterRegistry;

		private final Timer.Builder builder;

		MicroTimerBuilder(MeterRegistry meterRegistry, String name) {
			this.meterRegistry = meterRegistry;
			this.builder = Timer.builder(name);
		}

		@Override
		public MicroTimerBuilder tag(String key, String value) {
			this.builder.tag(key, value);
			return this;
		}

		@Override
		public MicroTimerBuilder description(String desc) {
			this.builder.description(desc);
			return this;
		}

		@Override
		public MicroTimer build() {
			return new MicroTimer(this.builder.register(this.meterRegistry), this.meterRegistry);
		}

	}

	private static abstract class AbstractMeter implements MeterFacade {

		protected final MeterRegistry meterRegistry; // NOSONAR

		protected AbstractMeter(MeterRegistry meterRegistry) {
			this.meterRegistry = meterRegistry;
		}

		/**
		 * Get the meter.
		 * @return the meter.
		 */
		protected abstract Meter getMeter();

		@SuppressWarnings("unchecked")
		@Override
		public <T extends MeterFacade> T remove() {
			if (this.meterRegistry.remove(getMeter()) != null) {
				return (T) this;
			}
			else {
				return null;
			}
		}

	}
	private static class MicroTimer extends AbstractMeter implements TimerFacade {

		private final Timer timer;

		MicroTimer(Timer timer, MeterRegistry meterRegistry) {
			super(meterRegistry);
			this.timer = timer;
		}

		@Override
		protected Meter getMeter() {
			return this.timer;
		}

		@Override
		public void record(long time, TimeUnit unit) {
			this.timer.record(time, unit);
		}

	}

	private static class MicroCounterBuilder implements CounterBuilder {

		private final MeterRegistry meterRegistry;

		private final Counter.Builder builder;

		MicroCounterBuilder(MeterRegistry meterRegistry, String name) {
			this.meterRegistry = meterRegistry;
			this.builder = Counter.builder(name);
		}

		@Override
		public CounterBuilder tag(String key, String value) {
			this.builder.tag(key, value);
			return this;
		}

		@Override
		public CounterBuilder description(String desc) {
			this.builder.description(desc);
			return this;
		}

		@Override
		public CounterFacade build() {
			return new MicroCounter(this.builder.register(this.meterRegistry), this.meterRegistry);
		}

	}

	private static class MicroCounter extends AbstractMeter implements CounterFacade {

		private final Counter counter;

		MicroCounter(Counter counter, MeterRegistry meterRegistry) {
			super(meterRegistry);
			this.counter = counter;
		}

		@Override
		protected Meter getMeter() {
			return this.counter;
		}

		@Override
		public void increment() {
			this.counter.increment();
		}

	}

	private static class MicroGaugeBuilder implements GaugeBuilder {

		private final MeterRegistry meterRegistry;

		private final Gauge.Builder<Object> builder;

		MicroGaugeBuilder(MeterRegistry meterRegistry, String name, Object obj, ToDoubleFunction<Object> f) {
			this.meterRegistry = meterRegistry;
			this.builder = Gauge.builder(name, obj, f);
		}

		@Override
		public GaugeBuilder tag(String key, String value) {
			this.builder.tag(key, value);
			return this;
		}

		@Override
		public GaugeBuilder description(String desc) {
			this.builder.description(desc);
			return this;
		}

		@Override
		public GaugeFacade build() {
			return new MicroGauge(this.builder.register(this.meterRegistry), this.meterRegistry);
		}

	}

	private static class MicroGauge extends AbstractMeter implements GaugeFacade {

		private final Gauge gauge;

		MicroGauge(Gauge gauge, MeterRegistry meterRegistry) {
			super(meterRegistry);
			this.gauge = gauge;
		}

		@Override
		protected Meter getMeter() {
			return this.gauge;
		}

	}

}
