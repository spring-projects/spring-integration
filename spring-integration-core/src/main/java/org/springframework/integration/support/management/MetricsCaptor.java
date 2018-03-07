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

package org.springframework.integration.support.management;

import java.util.function.ToDoubleFunction;

import org.springframework.lang.Nullable;

/**
 * @author Gary Russell
 * @since 5.0.4
 *
 */
public interface MetricsCaptor {

	TimerBuilder timerBuilder(String name);

	CounterBuilder counterBuilder(String name);

	GaugeBuilder gaugeBuilder(String name, @Nullable Object obj, ToDoubleFunction<Object> f);

	SampleFacade start();

	interface TimerBuilder {

		TimerBuilder tag(String key, String value);

		TimerBuilder description(String desc);

		TimerFacade build();

	}

	interface CounterBuilder {

		CounterBuilder tag(String key, String value);

		CounterBuilder description(String desc);

		CounterFacade build();

	}

	interface GaugeBuilder {

		GaugeBuilder tag(String key, String value);

		GaugeBuilder description(String desc);

		GaugeFacade build();

	}

}
