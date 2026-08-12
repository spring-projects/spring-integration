/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.http.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig(HttpOutboundGatewayWithMethodExpressionTests.MockServerConfig.class)
@DirtiesContext
public class HttpOutboundGatewayWithMethodExpressionTests {

	@Autowired
	private MessageChannel defaultChannel;

	@Autowired
	private MessageChannel requestChannel;

	@Autowired
	private PollableChannel replyChannel;

	@Autowired
	private MockRestServiceServer mockServer;

	@Configuration
	@ImportResource("classpath:org/springframework/integration/http/config/HttpOutboundGatewayWithMethodExpressionTests-context.xml")
	public static class MockServerConfig {

		@Bean
		public static RestClient.Builder restClientBuilder() {
			return RestClient.builder();
		}

		@Bean
		public static MockRestServiceServer mockServer(RestClient.Builder restClientBuilder) {
			return MockRestServiceServer.bindTo(restClientBuilder).build();
		}

		@Bean
		public static RestClient restClient(RestClient.Builder restClientBuilder, MockRestServiceServer mockServer) {
			return restClientBuilder.build();
		}

	}

	@BeforeEach
	public void setup() {
		this.mockServer.reset();
	}

	@Test
	public void testDefaultMethod() {
		this.mockServer.expect(requestTo("/testApps/httpMethod"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.defaultChannel.send(new GenericMessage<>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("POST");

		this.mockServer.verify();
	}

	@Test
	public void testExplicitlySetMethod() {
		this.mockServer.expect(requestTo("/testApps/httpMethod"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(HttpMethod.GET.name(), MediaType.TEXT_PLAIN));

		this.requestChannel.send(new GenericMessage<>("GET"));
		Message<?> message = replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("GET");

		this.mockServer.verify();
	}

	@Test
	public void testMutuallyExclusivityInMethodAndMethodExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(
						"http-outbound-gateway-with-httpmethod-expression-fail.xml", getClass()));
	}

}
