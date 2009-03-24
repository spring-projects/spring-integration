/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.integration.util.TestUtils.*;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.http.HttpInboundEndpoint;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class HttpInboundGatewayParserTests {

	@Autowired
	private HttpInboundEndpoint gateway;

	@Qualifier("responses")
	@Autowired
	MessageChannel responses;

	@Qualifier("requests")
	@Autowired
	SubscribableChannel requests;
	
	@Test
	public void checkConfig() {
		assertNotNull(gateway);
		assertThat((Boolean) getPropertyValue(gateway, "expectReply"), is(true));
	}
	
	@Test(timeout=1000)
	public void checkFlow() throws Exception {
		requests.subscribe(handlerExpecting(any(Message.class)));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setParameter("foo", "bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		gateway.handleRequest(request, response);
		assertThat(response.getStatus(), is(HttpServletResponse.SC_OK));
		assertThat(response.getContentType(), is("application/x-java-serialized-object"));
	}
}
