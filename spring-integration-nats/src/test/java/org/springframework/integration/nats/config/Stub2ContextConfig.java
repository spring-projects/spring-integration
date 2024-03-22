/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.nats.NatsMessageDrivenChannelAdapter;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dto.TestDTOStubV2;
import org.springframework.stereotype.Repository;

/**
 * Configuration class to setup Beans required to initialize NATS Message producers and consumers
 * using NATS integration classes
*
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 * href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 * all stakeholders and contact</a>
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
@Repository
public class Stub2ContextConfig extends GeneralContextConfig {

	@Bean
	public static MessageConverter<TestDTOStubV2> messageConvertorOfDTOStub() {
		return new MessageConverter<>(TestDTOStubV2.class);
	}

	@Bean
	public NatsMessageDrivenChannelAdapter testAdapter() {
		final NatsMessageDrivenChannelAdapter adapter =
				new NatsMessageDrivenChannelAdapter(testContainer(), messageConvertorOfDTOStub());
		adapter.setBeanName("testAdapter");
		adapter.setOutputChannel(consumerChannel());
		adapter.setErrorChannel(consumerErrorChannel());
		adapter.setAutoStartup(false);
		return adapter;
	}
}
