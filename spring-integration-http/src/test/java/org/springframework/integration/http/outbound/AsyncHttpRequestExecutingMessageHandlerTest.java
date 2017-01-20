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

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.http.HttpHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Shiliang Li
 * @since 5.0
 */
public class AsyncHttpRequestExecutingMessageHandlerTest {

	@Test
	public void testAsyncReturn() {
		AsyncHttpRequestExecutingMessageHandler asyncHandler = new AsyncHttpRequestExecutingMessageHandler(
				"http://www.springsource.org/spring-integration",
				new MockAsyncRestTemplate());
		QueueChannel ackChannel = MessageChannels.queue().get();
		asyncHandler.setOutputChannel(ackChannel);
		asyncHandler.handleMessage(MessageBuilder.withPayload("hello, world").build());
		Message<?> ack = ackChannel.receive(1000);
		assertNotNull(ack);
		assertNotNull(ack.getHeaders());
		assertEquals(ack.getHeaders().get(HttpHeaders.STATUS_CODE), HttpStatus.OK);
	}

	private static class MockAsyncRestTemplate extends AsyncRestTemplate {

		private final AtomicReference<HttpEntity<?>> lastRequestEntity = new AtomicReference<>();

		@Override
		public <T> ListenableFuture<ResponseEntity<T>> exchange(URI uri, HttpMethod method, HttpEntity<?> requestEntity, Class<T> responseType) throws RestClientException {
			this.lastRequestEntity.set(requestEntity);
			SettableListenableFuture<ResponseEntity<T>> futureReponse = new SettableListenableFuture<>();
			ResponseEntity<T> response = new ResponseEntity<>(HttpStatus.OK);
			futureReponse.set(response);

			return futureReponse;
		}
	}
}
