/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Properties;

import javax.jms.DeliveryMode;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.jms.StubMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 */
public class JmsOutboundGatewayParserTests {
	
	@Test
	public void testWithDelivertPersistentAttribute(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithDeliveryPersistent.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		int deliveryMode = (Integer)accessor.getPropertyValue("deliveryMode");
		assertEquals(DeliveryMode.PERSISTENT, deliveryMode);
	}

	@Test
	public void testDefault(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithConverter.xml", this.getClass());
		PollingConsumer endpoint = (PollingConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		MessageConverter converter = (MessageConverter)accessor.getPropertyValue("messageConverter");
		assertTrue("Wrong message converter", converter instanceof StubMessageConverter);
	}

	@Test
	public void gatewayWithOrder() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithOrder.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		Object order = accessor.getPropertyValue("order");
		assertEquals(99, order);
	}
	
	@Test
	public void gatewayMaintainsReplyChannelAndInboundHistory() {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"gatewayMaintainsReplyChannel.xml", this.getClass());
		SampleGateway gateway = context.getBean("gateway", SampleGateway.class);
		SubscribableChannel jmsInput = context.getBean("jmsInput", SubscribableChannel.class);
		MessageHandler handler = new MessageHandler() {	
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageHistory history = MessageHistory.read(message);
				assertNotNull(history);
				Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "inboundGateway", 0);
				assertNotNull(componentHistoryRecord);
				assertEquals("jms:inbound-gateway", componentHistoryRecord.get("type"));
				new MessagingTemplate((MessageChannel) message.getHeaders().getReplyChannel()).send(message);
			}
		};
		handler = spy(handler);
		jmsInput.subscribe(handler);
		String result = gateway.echo("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		assertEquals("hello", result);
	}
	
	public static interface SampleGateway{
		public String echo(String value);
	}
	
	public static class SampleService{
		public String echo(String value){
			return value.toUpperCase();
		}
	}
	
}
