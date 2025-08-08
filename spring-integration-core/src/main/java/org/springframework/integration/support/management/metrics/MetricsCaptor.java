/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.support.management.metrics;

import java.util.function.ToDoubleFunction;

import org.springframework.lang.Nullable;

/**
 * A metrics facade that delegates to a concrete implementation.
 *
 * @author Gary Russell
 * @since 5.0.4
 *
 */
public interface MetricsCaptor {

	/**
	 * Create a timer builder for a timer with the provided name.
	 * @param name the name.
	 * @return the builder.
	 */
	TimerBuilder timerBuilder(String name);

	/**
	 * Create a counter builder for a counter with the provided name.
	 * @param name the name.
	 * @return the builder.
	 */
	CounterBuilder counterBuilder(String name);

	/**
	 * Create a gauge builder for a gauge with the provided parameters.
	 * @param name the name.
	 * @param obj the object with which to invoke the function.
	 * @param f the function.
	 * @return the builder.
	 */
	GaugeBuilder gaugeBuilder(String name, @Nullable Object obj, ToDoubleFunction<Object> f);

	/**
	 * Start a sample collection.
	 * @return the sample.
	 */
	SampleFacade start();

	/**
	 * Remove a meter facade.
	 * @param facade the facade to remove.
	 * @return the removed facade, or null.
	 * @since 5.1
	 */
	@Nullable
	default MeterFacade removeMeter(MeterFacade facade) {
		return null;
	}

	/**
	 * A builder for a timer.
	 */
	interface TimerBuilder {

		/**
		 * Add a tag.
		 * @param key the key.
		 * @param value the value.
		 * @return the builder.
		 */
		TimerBuilder tag(String key, String value);

		/**
		 * Add the description.
		 * @param desc the description.
		 * @return the builder.
		 */
		TimerBuilder description(String desc);

		/**
		 * Build the timer.
		 * @return the timer.
		 */
		TimerFacade build();

	}

	/**
	 * A builder for a counter.
	 */
	interface CounterBuilder {

		/**
		 * Add a tag.
		 * @param key the key.
		 * @param value the value.
		 * @return the builder.
		 */
		CounterBuilder tag(String key, String value);

		/**
		 * Add the description.
		 * @param desc the description.
		 * @return the builder.
		 */
		CounterBuilder description(String desc);

		/**
		 * Build the counter.
		 * @return the counter.
		 */
		CounterFacade build();

	}

	/**
	 * A builder for a gauge.
	 */
	interface GaugeBuilder {

		/**
		 * Add a tag.
		 * @param key the key.
		 * @param value the value.
		 * @return the builder.
		 */
		GaugeBuilder tag(String key, String value);

		/**
		 * Add the description.
		 * @param desc the description.
		 * @return the builder.
		 */
		GaugeBuilder description(String desc);

		/**
		 * Build the gauge.
		 * @return the gauge.
		 */
		GaugeFacade build();

	}

}
