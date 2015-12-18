/*
 * Copyright 2002-2015 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
	public void testDefaultMethod() throws Exception {
		this.mockServer.expect(requestTo("/testApps/httpMethod"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess(HttpMethod.POST.name(), MediaType.TEXT_PLAIN));

		this.defaultChannel.send(new GenericMessage<String>("Hello"));
		Message<?> message = this.replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("POST", message.getPayload());

		this.mockServer.verify();
	}

	@Test
	public void testExplicitlySetMethod() throws Exception {
		this.mockServer.expect(requestTo("/testApps/httpMethod"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(HttpMethod.GET.name(), MediaType.TEXT_PLAIN));

		this.requestChannel.send(new GenericMessage<String>("GET"));
		Message<?> message = replyChannel.receive(5000);
		assertNotNull(message);
		assertEquals("GET", message.getPayload());

		this.mockServer.verify();
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testMutuallyExclusivityInMethodAndMethodExpression() throws Exception {
		new ClassPathXmlApplicationContext(
				"http-outbound-gateway-with-httpmethod-expression-fail.xml", getClass())
				.close();
	}

}
