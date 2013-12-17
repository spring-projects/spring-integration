/*
 * Copyright 2002-2013 the original author or authors.
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.Router;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.router.AbstractMappingMessageRouter;
import org.springframework.integration.router.MethodInvokingRouter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RouterParserTests {

	@Autowired
	private PollableChannel output1;

	@Autowired
	private PollableChannel output2;
	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel inputForRouterWithDefaultOutput;

	@Autowired
	private PollableChannel defaultOutput;

	@Autowired
	private MessageChannel inputForAbstractMessageRouterImplementation;

	@Autowired
	private PollableChannel output3;

	@Autowired
	private MessageChannel inputForAnnotatedRouter;

	@Autowired
	private PollableChannel output4;

	@Autowired
	private MessageChannel inputForRouterRequiringResolution;

	@Autowired
	private MessageChannel resolutionRequiredIsFalseInput;

	@Autowired
	@Qualifier("routerWithTimeout.handler")
	private MessageHandler routerWithTimeout;

	@Autowired
	private MessageChannel sequenceRouter;

	@Autowired
	private PollableChannel sequenceOut1;

	@Autowired
	private PollableChannel sequenceOut2;

	@Autowired
	private PollableChannel sequenceOut3;

	@Autowired
	private MessageChannel routerNestedBeanChannel;

	@Autowired
	private MessageChannel chainRouterNestedBeanChannel;

	@Autowired
	private MessageChannel routerAndErrorChannelInputChannel;

	@Autowired
	private SubscribableChannel errorChannel;

	@Test
	public void testRouter() {
		this.input.send(new GenericMessage<String>("1"));
		Message<?> result1 = this.output1.receive(1000);
		assertEquals("1", result1.getPayload());
		assertNull(output2.receive(0));
		input.send(new GenericMessage<String>("2"));
		Message<?> result2 = this.output2.receive(1000);
		assertEquals("2", result2.getPayload());
		assertNull(output1.receive(0));
	}

	@Test
	public void testRouterWithDefaultOutputChannel() {
		this.inputForRouterWithDefaultOutput.send(new GenericMessage<String>("99"));
		assertNull(this.output1.receive(0));
		assertNull(this.output2.receive(0));
		Message<?> result = this.defaultOutput.receive(0);
		assertEquals("99", result.getPayload());
	}

	@Test
	public void refOnlyForAbstractMessageRouterImplementation() {
		this.inputForAbstractMessageRouterImplementation.send(new GenericMessage<String>("test-implementation"));
		Message<?> result = this.output3.receive(1000);
		assertNotNull(result);
		assertEquals("test-implementation", result.getPayload());
	}

	@Test
	public void refOnlyForAnnotatedObject() {
		this.inputForAnnotatedRouter.send(new GenericMessage<String>("test-annotation"));
		Message<?> result = this.output4.receive(1000);
		assertNotNull(result);
		assertEquals("test-annotation", result.getPayload());
	}

	@Test
	public void testResolutionRequired() {
		try {
			this.inputForRouterRequiringResolution.send(new GenericMessage<Integer>(3));
		}
		catch (Exception e) {
			assertTrue(e.getCause() instanceof DestinationResolutionException);
		}
	}

	@Test(expected=MessageDeliveryException.class)
	public void testResolutionRequiredIsFalse() {
		this.resolutionRequiredIsFalseInput.send(new GenericMessage<String>("channelThatDoesNotExist"));
	}

	@Test
	public void timeoutValueConfigured() {
		assertTrue(this.routerWithTimeout instanceof MethodInvokingRouter);
		MessagingTemplate template = TestUtils.getPropertyValue(this.routerWithTimeout, "messagingTemplate", MessagingTemplate.class);
		Long timeout = TestUtils.getPropertyValue(template, "sendTimeout", Long.class);
		assertEquals(new Long(1234), timeout);
	}

	@Test
	public void sequence() {
		Message<?> originalMessage = new GenericMessage<String>("test");
		this.sequenceRouter.send(originalMessage);
		Message<?> message1 = this.sequenceOut1.receive(1000);
		Message<?> message2 = this.sequenceOut2.receive(1000);
		Message<?> message3 = this.sequenceOut3.receive(1000);
		assertEquals(originalMessage.getHeaders().getId(), new IntegrationMessageHeaderAccessor(message1).getCorrelationId());
		assertEquals(originalMessage.getHeaders().getId(), new IntegrationMessageHeaderAccessor(message2).getCorrelationId());
		assertEquals(originalMessage.getHeaders().getId(), new IntegrationMessageHeaderAccessor(message3).getCorrelationId());
		assertEquals(new Integer(1), new IntegrationMessageHeaderAccessor(message1).getSequenceNumber());
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(message1).getSequenceSize());
		assertEquals(new Integer(2), new IntegrationMessageHeaderAccessor(message2).getSequenceNumber());
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(message2).getSequenceSize());
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(message3).getSequenceNumber());
		assertEquals(new Integer(3), new IntegrationMessageHeaderAccessor(message3).getSequenceSize());
	}

	@Test
	public void testInt2893RouterNestedBean() {
		this.routerNestedBeanChannel.send(new GenericMessage<String>("1"));
		Message<?> result1 = this.output1.receive(1000);
		assertEquals("1", result1.getPayload());
		assertNull(this.output2.receive(0));
		this.routerNestedBeanChannel.send(new GenericMessage<String>("2"));
		Message<?> result2 = this.output2.receive(1000);
		assertEquals("2", result2.getPayload());
		assertNull(this.output1.receive(0));
	}

	@Test
	public void testInt2893RouterNestedBeanWithinChain() {
		this.chainRouterNestedBeanChannel.send(new GenericMessage<String>("1"));
		Message<?> result1 = this.output1.receive(1000);
		assertEquals("1", result1.getPayload());
		assertNull(this.output2.receive(0));
		this.chainRouterNestedBeanChannel.send(new GenericMessage<String>("2"));
		Message<?> result2 = this.output2.receive(1000);
		assertEquals("2", result2.getPayload());
		assertNull(this.output1.receive(0));
	}

	@Test
	public void testErrorChannel(){
		MessageHandler handler = mock(MessageHandler.class);
		this.errorChannel.subscribe(handler);
		this.routerAndErrorChannelInputChannel.send(new GenericMessage<String>("fail"));
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test // should not fail
	public void routerFactoryBeanTest(){
		new ClassPathXmlApplicationContext("rfb-fix-config.xml", this.getClass());
	}


	public static class NonExistingChannelRouter{
		public String route(String payload){
			return "foo";
		}
	}

	public static class TestRouterImplementation extends AbstractMappingMessageRouter {

		private final MessageChannel channel;

		public TestRouterImplementation(MessageChannel channel) {
			this.channel = channel;
		}


		@Override
		protected List<Object> getChannelKeys(Message<?> message) {
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


	static class TestChannelResover implements DestinationResolver<MessageChannel> {

		public MessageChannel resolveDestination(String channelName) {
			return null;
		}

	}

}
