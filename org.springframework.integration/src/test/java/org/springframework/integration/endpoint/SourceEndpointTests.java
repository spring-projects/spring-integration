/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageSource;

/**
 * @author Mark Fisher
 */
public class SourceEndpointTests {

	@Test
	public void testPolledSourceSendsToChannel() {
		TestSource source = new TestSource("testing", 1);
		QueueChannel channel = new QueueChannel();
		SourceEndpoint endpoint = new SourceEndpoint(source);
		endpoint.setTarget(channel);
		endpoint.afterPropertiesSet();
		endpoint.send(new TriggerMessage());
		Message<?> message = channel.receive(1000);
		assertNotNull("message should not be null", message);
		assertEquals("testing.1", message.getPayload());
	}


	private static class TestSource implements MessageSource<String> {

		private String message;

		private int limit;

		private AtomicInteger count = new AtomicInteger();

		public TestSource(String message, int limit) {
			this.message = message;
			this.limit = limit;
		}

		public void resetCounter() {
			this.count.set(0);
		}

		public Message<String> receive() {
			if (count.get() >= limit) {
				return null;
			}
			return new GenericMessage<String>(message + "." + count.incrementAndGet());
		}
	}

}
