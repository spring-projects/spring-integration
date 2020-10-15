/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.integration.support.management.micrometer;

import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
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
 * @author Artem Bilan
 *
 * @since 5.0.4
 *
 */
public class MicrometerMetricsCaptor implements MetricsCaptor {

	public static final String MICROMETER_CAPTOR_NAME = "integrationMicrometerMetricsCaptor";

	private MeterRegistry meterRegistry;

	private ObjectProvider<MeterRegistry> meterRegistryProvider;

	public MicrometerMetricsCaptor(MeterRegistry meterRegistry) {
		Assert.notNull(meterRegistry, "meterRegistry cannot be null");
		this.meterRegistry = meterRegistry;
	}

	MicrometerMetricsCaptor(ObjectProvider<MeterRegistry> meterRegistryProvider) {
		this.meterRegistryProvider = meterRegistryProvider;
	}

	public MeterRegistry getMeterRegistry() {
		if (this.meterRegistry == null) {
			this.meterRegistry = this.meterRegistryProvider.getIfUnique();
		}
		return this.meterRegistry;
	}

	@Override
	public TimerBuilder timerBuilder(String name) {
		return new MicroTimerBuilder(getMeterRegistry(), name);
	}

	@Override
	public CounterBuilder counterBuilder(String name) {
		return new MicroCounterBuilder(getMeterRegistry(), name);
	}

	@Override
	public GaugeBuilder gaugeBuilder(String name, Object obj, ToDoubleFunction<Object> f) {
		return new MicroGaugeBuilder(getMeterRegistry(), name, obj, f);
	}

	@Override
	public SampleFacade start() {
		return new MicroSample(Timer.start(getMeterRegistry()));
	}

	@Override
	public MeterFacade removeMeter(MeterFacade facade) {
		return facade.remove();
	}

	/**
	 * Add a MicrometerMetricsCaptor to the context if there's a MeterRegistry; if
	 * there's already a {@link MetricsCaptor} bean, return that.
	 * @param applicationContext the application context.
	 * @return the instance.
	 * @deprecated since 5.2.9 in favor of {@code @Import(MicrometerMetricsCaptorRegistrar.class)};
	 * will be removed in 6.0.
	 */
	@Deprecated
	public static MetricsCaptor loadCaptor(ApplicationContext applicationContext) {
		try {
			MeterRegistry registry = applicationContext.getBean(MeterRegistry.class);
			if (applicationContext instanceof GenericApplicationContext
					&& !applicationContext.containsBean(MICROMETER_CAPTOR_NAME)) {
				((GenericApplicationContext) applicationContext).registerBean(MICROMETER_CAPTOR_NAME,
						MicrometerMetricsCaptor.class,
						() -> new MicrometerMetricsCaptor(registry));
			}
			return applicationContext.getBean(MICROMETER_CAPTOR_NAME, MetricsCaptor.class);
		}
		catch (NoSuchBeanDefinitionException e) {
			return null;
		}
	}

	private static class MicroSample implements SampleFacade {

		private final Timer.Sample sample;

		MicroSample(Timer.Sample sample) {
			this.sample = sample;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void stop(TimerFacade timer) {
			this.sample.stop(((AbstractMeter<Timer>) timer).getMeter());
		}

	}

	protected static class MicroTimerBuilder implements TimerBuilder {

		protected final MeterRegistry meterRegistry; // NOSONAR

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

	protected abstract static class AbstractMeter<M extends Meter> implements MeterFacade {

		protected final MeterRegistry meterRegistry; // NOSONAR

		protected AbstractMeter(MeterRegistry meterRegistry) {
			this.meterRegistry = meterRegistry;
		}

		/**
		 * Get the meter.
		 * @return the meter.
		 */
		protected abstract M getMeter();

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

	protected static class MicroTimer extends AbstractMeter<Timer> implements TimerFacade {

		private final Timer timer;

		protected MicroTimer(Timer timer, MeterRegistry meterRegistry) {
			super(meterRegistry);
			this.timer = timer;
		}

		@Override
		protected Timer getMeter() {
			return this.timer;
		}

		@Override
		public void record(long time, TimeUnit unit) {
			this.timer.record(time, unit);
		}

		@Override
		public int hashCode() {
			return this.timer.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !getClass().equals(obj.getClass())) {
				return false;
			}
			return this.timer.equals(((MicroTimer) obj).timer);
		}

	}

	protected static class MicroCounterBuilder implements CounterBuilder {

		protected final MeterRegistry meterRegistry; // NOSONAR

		private final Counter.Builder builder;

		protected MicroCounterBuilder(MeterRegistry meterRegistry, String name) {
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

	protected static class MicroCounter extends AbstractMeter<Counter> implements CounterFacade {

		private final Counter counter;

		protected MicroCounter(Counter counter, MeterRegistry meterRegistry) {
			super(meterRegistry);
			this.counter = counter;
		}

		@Override
		protected Counter getMeter() {
			return this.counter;
		}

		@Override
		public void increment() {
			this.counter.increment();
		}

		@Override
		public int hashCode() {
			return this.counter.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !getClass().equals(obj.getClass())) {
				return false;
			}
			return this.counter.equals(((MicroCounter) obj).counter);
		}

	}

	protected static class MicroGaugeBuilder implements GaugeBuilder {

		protected final MeterRegistry meterRegistry; // NOSONAR

		private final Gauge.Builder<Object> builder;

		protected MicroGaugeBuilder(MeterRegistry meterRegistry, String name, Object obj, ToDoubleFunction<Object> f) {
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

	protected static class MicroGauge extends AbstractMeter<Gauge> implements GaugeFacade {

		private final Gauge gauge;

		protected MicroGauge(Gauge gauge, MeterRegistry meterRegistry) {
			super(meterRegistry);
			this.gauge = gauge;
		}

		@Override
		protected Gauge getMeter() {
			return this.gauge;
		}

		@Override
		public int hashCode() {
			return this.gauge.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !getClass().equals(obj.getClass())) {
				return false;
			}
			return this.gauge.equals(((MicroGauge) obj).gauge);
		}

	}

}
