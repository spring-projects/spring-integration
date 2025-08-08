/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class HttpOutboundGatewayWithMethodExpressionTests {

	@Autowired
	private MessageChannel defaultChannel;

	@Autowired
	private MessageChannel requestChannel;

	@Autowired
	private PollableChannel replyChannel;

	@Autowired
	private RestTemplate restTemplate;

	private MockRestServiceServer mockServer;

	@Before
	public void setup() {
		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
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

	@Test(expected = BeanDefinitionParsingException.class)
	public void testMutuallyExclusivityInMethodAndMethodExpression() {
		new ClassPathXmlApplicationContext(
				"http-outbound-gateway-with-httpmethod-expression-fail.xml", getClass())
				.close();
	}

}
