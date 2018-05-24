/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.integration.json;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.Default;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Artem Bilan
 *
 * @since 5.0
 */
@RunWith(SpringRunner.class)
public class ContentTypeConversionTests {

	@Autowired
	private AtomicReference<Object> sendData;

	@Autowired
	private ServiceGateway serviceGateway;

	@Test
	public void testContentTypeBasedConversion() {
		String json = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"age\":42}";
		TestPerson testPerson = this.serviceGateway.convertJsonToPerson(json);

		assertEquals(json, this.sendData.get());
		assertEquals("John", testPerson.getFirstName());
		assertEquals(42, testPerson.getAge());

		Map<String, String> map = Collections.singletonMap("foo", "bar");
		String result = this.serviceGateway.mapToString(map);
		assertEquals(map.toString(), result);
	}

	@MessagingGateway
	interface ServiceGateway {

		@Gateway(headers = @GatewayHeader(name = MessageHeaders.CONTENT_TYPE, value = "application/json"))
		TestPerson convertJsonToPerson(String jsonPerson);

		String mapToString(Map<?, ?> map);

	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public AtomicReference<Object> sendData() {
			return new AtomicReference<>();
		}

		@Bean
		public MessageChannel serviceChannel(final AtomicReference<Object> sendData) {
			return MessageChannels.direct()
					.interceptor(new ChannelInterceptor() {

						@Override
						public Message<?> preSend(Message<?> message, MessageChannel channel) {
							sendData.set(message.getPayload());
							return message;
						}

					})
					.get();
		}

		@Bean
		public IntegrationFlow serviceFlow() {
			return IntegrationFlows.from(ServiceGateway.class)
					.channel("serviceChannel")
					.handle(this)
					.get();
		}

		@ServiceActivator
		@Default
		public TestPerson handleJson(TestPerson person) {
			return person;
		}

		@ServiceActivator
		public String handleMap(@Payload Map<?, ?> map) {
			return map.toString();
		}

	}

}
