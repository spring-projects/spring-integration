/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint.annotation;

import org.junit.Test;

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.DocumentMessage;

/**
 * @author Mark Fisher
 */
public class AnnotationAwareMessageEndpointTests {

	@Test
	public void testSimpleHandler() throws InterruptedException {
		MessageBus messageBus = new MessageBus();
		messageBus.setAutoCreateChannels(true);
		AnnotationAwareMessageEndpoint endpoint =
				new AnnotationAwareMessageEndpoint(new SimpleEndpoint(), messageBus);
		endpoint.afterPropertiesSet();
		MessageChannel channel = messageBus.getChannel("testChannel");
		messageBus.start();
		endpoint.afterPropertiesSet();
		channel.send(new DocumentMessage(1, "world"), 10);
	}

	@MessageEndpoint(input="testChannel", pollPeriod=10)
	public class SimpleEndpoint {

		@Handler
		public void sayHello(String name) {
			System.out.println("hello " + name);
		}
	}
}
