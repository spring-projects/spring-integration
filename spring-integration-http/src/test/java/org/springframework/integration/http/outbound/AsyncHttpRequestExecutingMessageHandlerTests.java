/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.http.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

/**
 * @author Shiliang Li
 * @since 5.0
 */
public class AsyncHttpRequestExecutingMessageHandlerTests {

	@Test
	public void testAsyncReturn() {
		AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
		String destinationUri = "http://www.springsource.org/spring-integration";
		MockRestServiceServer
				.createServer(asyncRestTemplate)
				.expect(requestTo(destinationUri))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess());

		AsyncHttpRequestExecutingMessageHandler asyncHandler = new AsyncHttpRequestExecutingMessageHandler(
				destinationUri,
				asyncRestTemplate);
		QueueChannel ackChannel = MessageChannels.queue().get();
		asyncHandler.setOutputChannel(ackChannel);
		asyncHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());
		Message<?> ack = ackChannel.receive(1000);
		assertNotNull(ack);
		assertNotNull(ack.getHeaders());
		assertEquals(ack.getHeaders().get(HttpHeaders.STATUS_CODE), HttpStatus.OK);
	}
}
