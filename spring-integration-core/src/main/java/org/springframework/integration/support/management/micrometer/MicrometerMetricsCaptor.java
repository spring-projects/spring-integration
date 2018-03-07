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

import org.springframework.integration.support.management.CounterFacade;
import org.springframework.integration.support.management.GaugeFacade;
import org.springframework.integration.support.management.MetricsCaptor;
import org.springframework.integration.support.management.SampleFacade;
import org.springframework.integration.support.management.TimerFacade;
import org.springframework.util.Assert;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * @author Gary Russell
 * @since 5.0.4
 *
 */
public class MicrometerMetricsCaptor implements MetricsCaptor {

	private final MeterRegistry meterRegistry;

		public MicrometerMetricsCaptor(MeterRegistry meterRegistry) {
		Assert.notNull(meterRegistry, "meterRgistry cannot be null");
		this.meterRegistry = meterRegistry;
	}

	@Override
	public TimerBuilder timerBuilder(String name) {
		return new MicroTimerBuilder(name);
	}

	@Override
	public CounterBuilder counterBuilder(String name) {
		return new MicroCounterBuilder(name);
	}

	@Override
	public GaugeBuilder gaugeBuilder(String name, Object obj, ToDoubleFunction<Object> f) {
		return new MicroGaugeBuilder(name, obj, f);
	}

	@Override
	public SampleFacade start() {
		return new MicroSample(Timer.start(this.meterRegistry));
	}

	private class MicroSample implements SampleFacade {

		private final Timer.Sample sample;

		public MicroSample(Timer.Sample sample) {
			this.sample = sample;
		}

		@Override
		public void stop(TimerFacade timer) {
			this.sample.stop(((MicroTimer) timer).timer);
		}

	}

	private class MicroTimerBuilder implements TimerBuilder {

		private final Timer.Builder builder;

		MicroTimerBuilder(String name) {
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
			return new MicroTimer(this.builder.register(meterRegistry));
		}

	}

	private class MicroTimer implements TimerFacade {

		private final Timer timer;

		MicroTimer(Timer timer) {
			this.timer = timer;
		}

		@Override
		public SampleFacade start(MetricsCaptor captor) {
			return new MicroSample(Timer.start(MicrometerMetricsCaptor.this.meterRegistry));
		}

		@Override
		public void record(long time, TimeUnit unit) {
			this.timer.record(time, unit);
		}

	}

	private class MicroCounterBuilder implements CounterBuilder {

		private final Counter.Builder builder;

		MicroCounterBuilder(String name) {
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
			return new MicroCounter(this.builder.register(meterRegistry));
		}

	}

	private class MicroCounter implements CounterFacade {

		private final Counter counter;

		MicroCounter(Counter counter) {
			this.counter = counter;
		}

		@Override
		public void increment() {
			this.counter.increment();
		}

	}

	private class MicroGaugeBuilder implements GaugeBuilder {

		private final Gauge.Builder<Object> builder;

		MicroGaugeBuilder(String name, Object obj, ToDoubleFunction<Object> f) {
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
			this.builder.register(meterRegistry);
			return new MicroGauge();
		}

	}

	private class MicroGauge implements GaugeFacade {

	}

}
