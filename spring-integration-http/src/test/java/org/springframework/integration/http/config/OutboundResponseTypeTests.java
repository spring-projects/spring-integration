/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.2
 */
@SpringJUnitConfig
@DirtiesContext
public class OutboundResponseTypeTests {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private RestTemplate restTemplateWithConverters;

	@Autowired
	private QueueChannel replyChannel;

	@Autowired
	private MessageChannel requestChannel;

	@Autowired
	private MessageChannel resTypeSetChannel;

	@Autowired
	private MessageChannel resPrimitiveStringPresentationChannel;

	@Autowired
	private MessageChannel resTypeExpressionSetChannel;

	@Autowired
	private MessageChannel resTypeExpressionSetSerializationChannel;

	@Autowired
	private MessageChannel invalidResponseTypeChannel;

	@Autowired
	private MessageChannel contentTypePropagationChannel;

	private MockRestServiceServer mockServer;

	@BeforeEach
	public void setup() {
		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test
	public void testDefaultResponseType() {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.requestChannel.send(new GenericMessage<>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload() instanceof ResponseEntity).isTrue();

		this.mockServer.verify();
	}

	@Test
	public void testWithResponseTypeSet() {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeSetChannel.send(new GenericMessage<>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload() instanceof String).isTrue();

		this.mockServer.verify();
	}

	@Test
	public void testWithResponseTypeExpressionSet() {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeExpressionSetChannel.send(new GenericMessage<>("java.lang.String"));
		Message<?> message = this.replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload() instanceof String).isTrue();

		this.mockServer.verify();
	}

	@Test
	public void testWithResponseTypeExpressionSetAsClass() {
		this.mockServer = MockRestServiceServer.createServer(this.restTemplateWithConverters);
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeExpressionSetSerializationChannel.send(new GenericMessage<Class<?>>(String.class));
		Message<?> message = this.replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload() instanceof String).isTrue();

		this.mockServer.verify();
	}

	@Test
	public void testInt2706ResponseTypeExpressionAsPrimitive() {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeExpressionSetChannel.send(new GenericMessage<>("byte[]"));
		Message<?> message = this.replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload() instanceof byte[]).isTrue();

		this.mockServer.verify();
	}

	@Test
	public void testInt2706ResponseTypePrimitiveArrayClassAsString() {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resPrimitiveStringPresentationChannel.send(new GenericMessage<>("hello".getBytes()));
		Message<?> message = this.replyChannel.receive(5000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload() instanceof byte[]).isTrue();

		this.mockServer.verify();
	}

	@Test
	public void testInt3052InvalidResponseType() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.invalidResponseTypeChannel.send(new GenericMessage<>("hello".getBytes())))
				.withCauseInstanceOf(IllegalStateException.class)
				.withStackTraceContaining("'expectedResponseType' can be an instance of " +
						"'Class<?>', 'String' or 'ParameterizedTypeReference<?>'");
	}

	@Test
	public void testMutuallyExclusivityInMethodAndMethodExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("OutboundResponseTypeTests-context-fail.xml",
						getClass()))
				.withMessageContaining("The 'expected-response-type' " +
						"and 'expected-response-type-expression' are mutually exclusive");
	}

	@Test
	public void testContentTypePropagation() {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.contentTypePropagationChannel
				.send(new GenericMessage<>(Collections.singletonMap("foo", "bar")));

		this.mockServer.verify();
	}

	@Test
	public void notAllowedEncodingModeWhenExternalRestTemplate() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext(
						"OutboundResponseTypeTests-context-encoding-mode-fail.xml", getClass()))
				.withMessageContaining("When providing a 'rest-template' reference, " +
						"the 'encoding-mode' must be set on the 'RestTemplate.uriTemplateHandler' property.");
	}

}
