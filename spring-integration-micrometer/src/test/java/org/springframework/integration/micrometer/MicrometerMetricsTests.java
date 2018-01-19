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

package org.springframework.integration.micrometer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Gary Russell
 * @since 5.0.1
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class MicrometerMetricsTests {

	@Autowired
	private MeterRegistry meterRegistry;

	@Autowired
	private AbstractMessageChannel channel;

	@Autowired
	@Qualifier("micrometerMetricsTests.Config.service.serviceActivator.handler")
	private AbstractMessageHandler handler;

	@Test
	public void testSend() {
		this.channel.send(new GenericMessage<>("foo"));
		List<Meter> meters = this.meterRegistry.getMeters();
		int foundTimers = 0;
		for (Meter meter : meters) {
			String name = meter.getId().getName();
			if (name.equals("channel.timer")
					|| name.equals("micrometerMetricsTests.Config.service.serviceActivator.handler.timer")) {
				assertThat(((Timer) meter).count()).isEqualTo(1L);
				foundTimers++;
			}
		}
		assertThat(foundTimers).isEqualTo(2);
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	public static class Config {

		@Bean
		public MicrometerMetricsFactory metricsFactory() {
			return new MicrometerMetricsFactory(meterRegistry());
		}

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@ServiceActivator(inputChannel = "channel")
		public void service(String in) {

		}

	}

}
