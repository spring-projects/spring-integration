/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jms.config;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.core.MessagingTemplate;

/**
 * @author ozhurakousky
 * @author Gunnar Hillert
 *
 */
public class ExtractRequestReplyPayloadTests {
	ClassPathXmlApplicationContext applicationContext;
	MessageChannel outboundChannel;
	SubscribableChannel jmsInputChannel;
	PollableChannel replyChannel;
	@Before
	public void prepare(){
		ActiveMqTestUtils.prepare();
		applicationContext = new ClassPathXmlApplicationContext("ExtractRequestReplyPayloadTests-context.xml", this.getClass());
		outboundChannel = applicationContext.getBean("outboundChannel", MessageChannel.class);
		jmsInputChannel = applicationContext.getBean("jmsInputChannel", SubscribableChannel.class);
		replyChannel = applicationContext.getBean("replyChannel", PollableChannel.class);
	}
	@After
	public void cleanup(){
		applicationContext.destroy();
	}

	@Test
	public void testOutboundInboundDefault(){
		jmsInputChannel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		});
		outboundChannel.send(new GenericMessage<String>("Hello"));

		Message<?> replyMessage = replyChannel.receive(1000);
		assertTrue(replyMessage.getPayload() instanceof String);
	}

	@Test
	public void testOutboundBothFalseInboundDefault(){

		JmsOutboundGateway outboundGateway =
			(JmsOutboundGateway) new DirectFieldAccessor(applicationContext.getBean("outboundGateway")).getPropertyValue("handler");
		outboundGateway.setExtractRequestPayload(false);
		outboundGateway.setExtractReplyPayload(false);

		jmsInputChannel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		});
		outboundChannel.send(new GenericMessage<String>("Hello"));

		Message<?> replyMessage = replyChannel.receive(1000);
		assertTrue(replyMessage.getPayload() instanceof javax.jms.Message);
	}
	@Test(expected=MessageTimeoutException.class)
	public void testOutboundDefaultInboundBothTrue(){

		ChannelPublishingJmsMessageListener inboundGateway =
			(ChannelPublishingJmsMessageListener)new DirectFieldAccessor(applicationContext.getBean("inboundGateway")).
																	getPropertyValue("listener");
		inboundGateway.setExtractReplyPayload(false);
		inboundGateway.setExtractRequestPayload(false);

		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof javax.jms.Message);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		};
		handler = spy(handler);
		jmsInputChannel.subscribe(handler);
		outboundChannel.send(new GenericMessage<String>("Hello"));
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		replyChannel.receive(1000);
	}
	@Test
	public void testOutboundDefaultInboundReplyTrueRequestFalse(){

		ChannelPublishingJmsMessageListener inboundGateway =
			(ChannelPublishingJmsMessageListener)new DirectFieldAccessor(applicationContext.getBean("inboundGateway")).
																	getPropertyValue("listener");
		inboundGateway.setExtractReplyPayload(true);
		inboundGateway.setExtractRequestPayload(false);

		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof javax.jms.Message);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		};
		jmsInputChannel.subscribe(handler);
		outboundChannel.send(new GenericMessage<String>("Hello"));
		Message<?> replyMessage = replyChannel.receive(1000);
		assertTrue(replyMessage.getPayload() instanceof String);
	}
	@Test
	public void testOutboundDefaultInboundReplyFalseRequestTrue(){

		ChannelPublishingJmsMessageListener inboundGateway =
			(ChannelPublishingJmsMessageListener)new DirectFieldAccessor(applicationContext.getBean("inboundGateway")).
																	getPropertyValue("listener");
		inboundGateway.setExtractReplyPayload(false);
		inboundGateway.setExtractRequestPayload(true);

		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		};
		jmsInputChannel.subscribe(handler);
		outboundChannel.send(new GenericMessage<String>("Hello"));
		Message<?> replyMessage = replyChannel.receive(1000);
		assertTrue(replyMessage.getPayload() instanceof String);
	}
	@Test
	public void testOutboundRequestTrueReplyFalseInboundDefault(){
		JmsOutboundGateway outboundGateway =
			(JmsOutboundGateway) new DirectFieldAccessor(applicationContext.getBean("outboundGateway")).getPropertyValue("handler");
		outboundGateway.setExtractRequestPayload(true);
		outboundGateway.setExtractReplyPayload(false);

		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		};
		jmsInputChannel.subscribe(handler);
		outboundChannel.send(new GenericMessage<String>("Hello"));
		Message<?> replyMessage = replyChannel.receive(1000);
		assertTrue(replyMessage.getPayload() instanceof javax.jms.Message);
	}
	@Test
	public void testOutboundRequestFalseReplyTrueInboundDefault(){
		JmsOutboundGateway outboundGateway =
			(JmsOutboundGateway) new DirectFieldAccessor(applicationContext.getBean("outboundGateway")).getPropertyValue("handler");
		outboundGateway.setExtractRequestPayload(false);
		outboundGateway.setExtractReplyPayload(true);

		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof String);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		};
		jmsInputChannel.subscribe(handler);
		outboundChannel.send(new GenericMessage<String>("Hello"));
		Message<?> replyMessage = replyChannel.receive(1000);
		assertTrue(replyMessage.getPayload() instanceof String);
	}
	@Test(expected=MessageTimeoutException.class)
	public void testAllFalse(){
		JmsOutboundGateway outboundGateway =
			(JmsOutboundGateway) new DirectFieldAccessor(applicationContext.getBean("outboundGateway")).getPropertyValue("handler");
		outboundGateway.setExtractRequestPayload(false);
		outboundGateway.setExtractReplyPayload(false);

		ChannelPublishingJmsMessageListener inboundGateway =
			(ChannelPublishingJmsMessageListener)new DirectFieldAccessor(applicationContext.getBean("inboundGateway")).
																	getPropertyValue("listener");
		inboundGateway.setExtractReplyPayload(false);
		inboundGateway.setExtractRequestPayload(false);

		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				assertTrue(message.getPayload() instanceof javax.jms.Message);
				MessagingTemplate template = new MessagingTemplate();
				template.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
				template.send(message);
			}
		};
		jmsInputChannel.subscribe(handler);
		outboundChannel.send(new GenericMessage<String>("Hello"));
		Message<?> replyMessage = replyChannel.receive(1000);
		assertTrue(replyMessage.getPayload() instanceof String);
	}
}
