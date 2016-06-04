/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.http.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Collections;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.BeansException;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.2
 *
 * <p>
 * see https://jira.springsource.org/browse/INT-2397
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Before
	public void setup() {
		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
	}

	@Test
	public void testDefaultResponseType() throws Exception {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.requestChannel.send(new GenericMessage<String>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof ResponseEntity);

		this.mockServer.verify();
	}

	@Test
	public void testWithResponseTypeSet() throws Exception {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeSetChannel.send(new GenericMessage<String>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);

		this.mockServer.verify();
	}

	@Test
	public void testWithResponseTypeExpressionSet() throws Exception {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeExpressionSetChannel.send(new GenericMessage<String>("java.lang.String"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);

		this.mockServer.verify();
	}

	@Test
	public void testWithResponseTypeExpressionSetAsClass() throws Exception {
		this.mockServer = MockRestServiceServer.createServer(this.restTemplateWithConverters);
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeExpressionSetSerializationChannel.send(new GenericMessage<Class<?>>(String.class));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof String);

		this.mockServer.verify();
	}

	@Test
	public void testInt2706ResponseTypeExpressionAsPrimitive() throws Exception {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resTypeExpressionSetChannel.send(new GenericMessage<String>("byte[]"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof byte[]);

		this.mockServer.verify();
	}

	@Test
	public void testInt2706ResponseTypePrimitiveArrayClassAsString() throws Exception {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.resPrimitiveStringPresentationChannel.send(new GenericMessage<byte[]>("hello".getBytes()));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof byte[]);

		this.mockServer.verify();
	}

	@Test
	public void testInt3052InvalidResponseType() throws Exception {
		try {
			this.invalidResponseTypeChannel.send(new GenericMessage<byte[]>("hello".getBytes()));
			fail("IllegalStateException expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessageHandlingException.class));
			Throwable t = e.getCause();
			assertThat(t, Matchers.instanceOf(IllegalStateException.class));
			assertThat(t.getMessage(), Matchers.containsString("'expectedResponseType' can be an instance of " +
					"'Class<?>', 'String' or 'ParameterizedTypeReference<?>'"));
		}
	}

	@Test
	public void testMutuallyExclusivityInMethodAndMethodExpression() throws Exception {
		try {
			new ClassPathXmlApplicationContext("OutboundResponseTypeTests-context-fail.xml", this.getClass()).close();
			fail("Expected BeansException");
		}
		catch (BeansException e) {
			assertTrue(e instanceof BeanDefinitionParsingException);
			assertTrue(e.getMessage().contains("The 'expected-response-type' " +
					"and 'expected-response-type-expression' are mutually exclusive"));
		}
	}

	@Test
	public void testContentTypePropagation() throws Exception {
		this.mockServer.expect(requestTo("/testApps/outboundResponse"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString()))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.contentTypePropagationChannel
				.send(new GenericMessage<Map<String, String>>(Collections.singletonMap("foo", "bar")));

		this.mockServer.verify();
	}

}
