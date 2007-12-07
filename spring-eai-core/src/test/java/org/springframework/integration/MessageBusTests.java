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

package org.springframework.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.message.DocumentMessage;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class MessageBusTests {
/*
	@Test
	public void testStandaloneWithEndpoint() {
		MessageBus bus = new MessageBus();
		MessageChannel sourceChannel = new PointToPointChannel();
		MessageChannel targetChannel = new PointToPointChannel();
		bus.registerChannel("sourceChannel", sourceChannel);
		sourceChannel.send(new DocumentMessage("123", "test"));
		bus.registerChannel("targetChannel", targetChannel);
		GenericMessageEndpoint endpoint = new GenericMessageEndpoint(sourceChannel);
		endpoint.setTarget(targetChannel);
		bus.registerEndpoint("endpoint", endpoint);
		Message result = targetChannel.receive();
		assertEquals("test", result.getPayload());
	}

	@Test
	public void testStandaloneWithoutEndpoint() {
		MessageBus bus = new MessageBus();
		MessageChannel sourceChannel = new PointToPointChannel();
		sourceChannel.send(new DocumentMessage("123", "test"));
		MessageChannel targetChannel = new PointToPointChannel();
		bus.registerChannel("sourceChannel", sourceChannel);
		bus.registerChannel("targetChannel", targetChannel);
		Message result = targetChannel.receive(10);
		assertNull(result);
	}
*/

	@Test
	public void testAutodetectionWithApplicationContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("messageBusTests.xml", this.getClass());
		context.start();
		MessageChannel sourceChannel = (MessageChannel) context.getBean("sourceChannel");
		sourceChannel.send(new DocumentMessage("123", "test"));		
		MessageChannel targetChannel = (MessageChannel) context.getBean("targetChannel");
		Message result = targetChannel.receive(10);
		assertEquals("test", result.getPayload());
	}

}
