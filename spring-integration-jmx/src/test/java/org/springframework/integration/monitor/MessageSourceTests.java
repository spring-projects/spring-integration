/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.monitor;

import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.endpoint.AbstractFetchLimitingMessageSource;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class MessageSourceTests {

	@Autowired
	private MBeanServer server;

	@Autowired
	private MaxFetchSource source1;

	@Autowired
	private MaxFetchSource source2;

	@Test
	public void testMaxFetch() throws Exception {
		Set<ObjectName> query = this.server.queryNames(new ObjectName("foo:type=MessageSource,*"), null);
		for (ObjectName instance : query) {
			assertThat(this.server.getAttribute(instance, "MaxFetchSize")).isEqualTo(123);
			this.server.setAttribute(instance, new Attribute("MaxFetchSize", 456));
			assertThat(this.server.getAttribute(instance, "MaxFetchSize")).isEqualTo(456);
		}
		assertThat(this.source1.getMaxFetchSize()).isEqualTo(456);
		assertThat(this.source2.getMaxFetchSize()).isEqualTo(456);
	}

	@Configuration
	@EnableIntegrationMBeanExport(server = "mbeanServer", defaultDomain = "foo")
	@EnableIntegration
	public static class Config {

		@Bean
		public MBeanServerFactoryBean mbeanServer() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		@InboundChannelAdapter(channel = "out")
		public MaxFetchSource source1() {
			return new MaxFetchSource();
		}

		@Bean
		public MaxFetchSource source2() { // raw source, no consumer
			return new MaxFetchSource();
		}

		@Bean
		public PollableChannel out() {
			return new QueueChannel();
		}

		@Bean(name = PollerMetadata.DEFAULT_POLLER)
		public PollerMetadata defaultPoller() {
			PollerMetadata pollerMetadata = new PollerMetadata();
			pollerMetadata.setTrigger(t -> null);
			return pollerMetadata;
		}

	}

	public static class MaxFetchSource extends AbstractFetchLimitingMessageSource<String> {

		@Override
		protected void onInit() {
			super.onInit();
			setMaxFetchSize(123);
		}

		@Override
		public String getComponentType() {
			return null;
		}

		@Override
		protected Object doReceive(int maxFetchSize) {
			return null;
		}

	}

}
