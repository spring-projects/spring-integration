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

package org.springframework.integration.transformer;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class MessageTransformingChannelInterceptorTests {
	
	AbstractMessageChannel channel; 
	
	StringMessage message;
	
	TestTransformer transfomer;
	
	MessageTransformingChannelInterceptor channelInterceptor;
	
	@Before
	public void setUp(){
		channel = new QueueChannel();
		message = new StringMessage("test");
		transfomer = new TestTransformer();
		channelInterceptor = new MessageTransformingChannelInterceptor(transfomer);
		channel.addInterceptor(channelInterceptor);
	}
	
	@Test
	public void testTransformOnReceive(){
		channelInterceptor.setConvertOnSend(false);
		channel.send(message);
		assertFalse("Transfomrer on incorrectly invoked on send", transfomer.invoked);
		Message msg = channel.receive(1);
		assertEquals("Wrong message",message, msg);
		assertTrue("Transfomer not invoked on receive", transfomer.invoked);
	}
	
	@Test
	public void testTransformOnSend(){
		channelInterceptor.setConvertOnSend(true);
		channel.send(message);
		assertTrue("Transfomrer not invoked on send", transfomer.invoked);
		Message msg = channel.receive(1);
		assertEquals("Wrong message",message, msg);
		assertEquals("Transfomer invoked on receive", 1, transfomer.invokedCount);
	}
	
	
	private static class TestTransformer implements MessageTransformer{

		boolean invoked = false;
		
		int invokedCount = 0;
		
		public void transform(Message<?> message) {
			invoked = true;
			invokedCount++;
		}
		
	}

}
