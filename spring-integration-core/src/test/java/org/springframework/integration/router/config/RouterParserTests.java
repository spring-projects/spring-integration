/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.support.channel.ChannelResolver;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Gunnar Hillert
 */
public class RouterParserTests {

	@Test
	public void testRouter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("input");
		PollableChannel output1 = (PollableChannel) context.getBean("output1");
		PollableChannel output2 = (PollableChannel) context.getBean("output2");
		input.send(new GenericMessage<String>("1"));
		Message<?> result1 = output1.receive(1000);
		assertEquals("1", result1.getPayload());
		assertNull(output2.receive(0));
		input.send(new GenericMessage<String>("2"));
		Message<?> result2 = output2.receive(1000);
		assertEquals("2", result2.getPayload());
		assertNull(output1.receive(0));
	}

	@Test
	public void testRouterWithDefaultOutputChannel() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("inputForRouterWithDefaultOutput");
		PollableChannel output1 = (PollableChannel) context.getBean("output1");
		PollableChannel output2 = (PollableChannel) context.getBean("output2");
		PollableChannel defaultOutput = (PollableChannel) context.getBean("defaultOutput");
		input.send(new GenericMessage<String>("99"));
		assertNull(output1.receive(0));
		assertNull(output2.receive(0));
		Message<?> result = defaultOutput.receive(0);
		assertEquals("99", result.getPayload());
	}

	@Test
	public void refOnlyForAbstractMessageRouterImplementation() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("inputForAbstractMessageRouterImplementation");
		PollableChannel output = (PollableChannel) context.getBean("output3");
		input.send(new GenericMessage<String>("test-implementation"));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertEquals("test-implementation", result.getPayload());		
	}

	@Test
	public void refOnlyForAnnotatedObject() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("inputForAnnotatedRouter");
		PollableChannel output = (PollableChannel) context.getBean("output4");
		input.send(new GenericMessage<String>("test-annotation"));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertEquals("test-annotation", result.getPayload());	
	}
	
	@Test(expected=MessageDeliveryException.class)
	public void testResolutionRequired() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("inputForRouterRequiringResolution");
		input.send(new GenericMessage<Integer>(3));
	}

	public void testChannelResolutionRequiredIsTrue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("channelResolutionRequiredIsTrueInput");
		input.send(new GenericMessage<String>("channelThatDoesNotExist"));
	}

	@Test
	public void timeoutValueConfigured() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		Object endpoint = context.getBean("routerWithTimeout");
		MethodInvokingRouter router = TestUtils.getPropertyValue(endpoint, "handler", MethodInvokingRouter.class);
		MessagingTemplate template = (MessagingTemplate)
				new DirectFieldAccessor(router).getPropertyValue("messagingTemplate");
		Long timeout = (Long) new DirectFieldAccessor(template).getPropertyValue("sendTimeout");
		assertEquals(new Long(1234), timeout);
	}

	@Test
	public void channelResolverConfigured() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		Object channelResolverBean = context.getBean("testChannelResolver");
		Object endpoint = context.getBean("routerWithChannelResolver");
		MethodInvokingRouter router = TestUtils.getPropertyValue(endpoint, "handler", MethodInvokingRouter.class);
		ChannelResolver channelResolver = (ChannelResolver)
				new DirectFieldAccessor(router).getPropertyValue("channelResolver");
		assertSame(channelResolverBean, channelResolver);
	}

	@Test
	public void sequence() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		MessageChannel input = context.getBean("sequenceRouter", MessageChannel.class);
		PollableChannel out1 = context.getBean("sequenceOut1", PollableChannel.class);
		PollableChannel out2 = context.getBean("sequenceOut2", PollableChannel.class);
		PollableChannel out3 = context.getBean("sequenceOut3", PollableChannel.class);
		Message<?> originalMessage = new GenericMessage<String>("test");
		input.send(originalMessage);
		Message<?> message1 = out1.receive(0);
		Message<?> message2 = out2.receive(0);
		Message<?> message3 = out3.receive(0);
		assertEquals(originalMessage.getHeaders().getId(), message1.getHeaders().getCorrelationId());
		assertEquals(originalMessage.getHeaders().getId(), message2.getHeaders().getCorrelationId());
		assertEquals(originalMessage.getHeaders().getId(), message3.getHeaders().getCorrelationId());
		assertEquals(new Integer(1), message1.getHeaders().getSequenceNumber());
		assertEquals(new Integer(3), message1.getHeaders().getSequenceSize());
		assertEquals(new Integer(2), message2.getHeaders().getSequenceNumber());
		assertEquals(new Integer(3), message2.getHeaders().getSequenceSize());
		assertEquals(new Integer(3), message3.getHeaders().getSequenceNumber());
		assertEquals(new Integer(3), message3.getHeaders().getSequenceSize());
	}

	@Test
	public void testErrorChannel(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"ErrorChannelRoutingTests-context.xml", this.getClass());
		MessageHandler handler = mock(MessageHandler.class);
		DirectChannel inputChannel = context.getBean("inputChannel", DirectChannel.class);
		SubscribableChannel errorChannel = context.getBean("errorChannel", SubscribableChannel.class);
		errorChannel.subscribe(handler);
		inputChannel.send(new GenericMessage<String>("fail"));
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	public static class NonExistingChannelRouter{
		public String route(String payload){
			return "foo";
		}
	}

	public static class TestRouterImplementation extends AbstractMessageRouter {

		private final MessageChannel channel;

		public TestRouterImplementation(MessageChannel channel) {
			this.channel = channel;
		}


		@Override
		protected List<Object> getChannelIdentifiers(Message<?> message) {
			return Collections.singletonList((Object)this.channel);
		}
	}


	public static class AnnotatedTestRouterBean {

		private final MessageChannel channel;

		public AnnotatedTestRouterBean(MessageChannel channel) {
			this.channel = channel;
		}

		@Router
		public MessageChannel test(String payload) {
			return this.channel;
		}
	}
	
	public static class ReturnStringPassedInAsChannelNameRouter {

		@Router
		public String route(Message<?> message) {
			return (String)message.getPayload();
		}

		
	}


	static class TestChannelResover implements ChannelResolver {

		public MessageChannel resolveChannelName(String channelName) {
			return null;
		}

	}

}
