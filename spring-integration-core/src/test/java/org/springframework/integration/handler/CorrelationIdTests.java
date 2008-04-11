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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.router.SplitterMessageHandlerAdapter;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class CorrelationIdTests {

	@Test
	public void testCorrelationIdPassedIfAvailable() {
		Object correlationId = "123-ABC";
		Message<?> message = new StringMessage("test");
		message.getHeader().setCorrelationId(correlationId);
		DefaultMessageHandlerAdapter<TestBean> adapter = new DefaultMessageHandlerAdapter<TestBean>();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals(correlationId, reply.getHeader().getCorrelationId());
	}

	@Test
	public void testCorrelationIdCopiedFromMessageIdByDefault() {
		Message<?> message = new StringMessage("test");
		DefaultMessageHandlerAdapter<TestBean> adapter = new DefaultMessageHandlerAdapter<TestBean>();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals(message.getId(), reply.getHeader().getCorrelationId());
	}

	@Test
	public void testCorrelationIdCopiedFromMessageCorrelationIdIfAvailable() {
		Message<?> message = new StringMessage("messageId","test");
		message.getHeader().setCorrelationId("correlationId");
		DefaultMessageHandlerAdapter<TestBean> adapter = new DefaultMessageHandlerAdapter<TestBean>();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals(message.getHeader().getCorrelationId(), reply.getHeader().getCorrelationId());
		assertTrue(message.getHeader().getCorrelationId().equals(reply.getHeader().getCorrelationId()));
	}

	@Test
	public void testCorrelationNotPassedIfAlreadySetByHandler() throws Exception {
		Object correlationId = "123-ABC";
		Message<?> message = new StringMessage("test");
		message.getHeader().setCorrelationId(correlationId);
		AbstractMessageHandlerAdapter<TestBean> adapter = new AbstractMessageHandlerAdapter<TestBean>() {
			@Override
			protected Object doHandle(Message<?> message, HandlerMethodInvoker<TestBean> invoker) {
				Object result = invoker.invokeMethod(message.getPayload());
				Message<?> resultMessage = new GenericMessage<Object>(result);
				resultMessage.getHeader().setCorrelationId("456-XYZ");
				return resultMessage;
			}
		};
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals("456-XYZ", reply.getHeader().getCorrelationId());
	}

	@Test
	public void testCorrelationNotCopiedIfAlreadySetByHandler() throws Exception {
		Message<?> message = new StringMessage("test");
		AbstractMessageHandlerAdapter<TestBean> adapter = new AbstractMessageHandlerAdapter<TestBean>() {
			@Override
			protected Object doHandle(Message<?> message, HandlerMethodInvoker<TestBean> invoker) {
				Object result = invoker.invokeMethod(message.getPayload());
				Message<?> resultMessage = new GenericMessage<Object>(result);
				resultMessage.getHeader().setCorrelationId("456-XYZ");
				return resultMessage;
			}
		};
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		Message<?> reply = adapter.handle(message);
		assertEquals("456-XYZ", reply.getHeader().getCorrelationId());
	}

	@Test
	public void testCorrelationIdWithSplitter() throws Exception {
		Message<?> message = new StringMessage("test1,test2");
		DefaultMessageHandlerAdapter<TestBean> adapter = new DefaultMessageHandlerAdapter<TestBean>();
		adapter.setObject(new TestBean());
		adapter.setMethodName("upperCase");
		adapter.afterPropertiesSet();
		MessageChannel testChannel = new SimpleChannel();
		ChannelRegistry channelRegistry = new DefaultChannelRegistry();
		channelRegistry.registerChannel("testChannel", testChannel);
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(AbstractMessageHandlerAdapter.DEFAULT_OUTPUT_CHANNEL_NAME_KEY, "testChannel");
		SplitterMessageHandlerAdapter<TestBean> splitter = new SplitterMessageHandlerAdapter<TestBean>(
				new TestBean(), TestBean.class.getMethod("split", String.class), attributes);
		splitter.setChannelRegistry(channelRegistry);
		splitter.afterPropertiesSet();
		splitter.handle(message);
		Message<?> reply1 = testChannel.receive(100);
		Message<?> reply2 = testChannel.receive(100);
		assertEquals(message.getId(), reply1.getHeader().getCorrelationId());
		assertEquals(message.getId(), reply2.getHeader().getCorrelationId());		
	}


	private static class TestBean {

		public String upperCase(String input) {
			return input.toUpperCase();
		}

		public String[] split(String input) {
			return input.split(",");
		}
	}

}
