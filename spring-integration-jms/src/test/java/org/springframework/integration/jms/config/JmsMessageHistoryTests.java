/*
 * Copyright 2002-2010 the original author or authors.
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
import static junit.framework.Assert.assertEquals;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.MessageHistoryEvent;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.StringMessage;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 *
 */
public class JmsMessageHistoryTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testInboundAdapter() throws Exception{
		ActiveMqTestUtils.prepare();
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext("MessageHistoryTests-context.xml", JmsMessageHistoryTests.class);
		SampleGateway gateway = applicationContext.getBean("sampleGateway", SampleGateway.class);
		PollableChannel jmsInputChannel = applicationContext.getBean("jmsInputChannel", PollableChannel.class);
		gateway.send("hello");
		Message<String> message = (Message<String>) jmsInputChannel.receive(5000);
		Iterator<MessageHistoryEvent> historyIterator = message.getHeaders().getHistory().iterator();
		MessageHistoryEvent event = historyIterator.next();
		assertEquals("jms:inbound-channel-adapter", event.getType());
		assertEquals("sampleJmsInboundAdapter", event.getName());
		event = historyIterator.next();
		assertEquals("channel", event.getType());
		assertEquals("jmsInputChannel", event.getName());
	}
	@SuppressWarnings("unchecked")
	@Test
	public void testWithHeaderMapperPropagatingOutboundHistory() throws Exception{
		ActiveMqTestUtils.prepare();
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext("MessageHistoryTests-withHeaderMapper.xml", JmsMessageHistoryTests.class);
		DirectChannel input = applicationContext.getBean("outbound-channel", DirectChannel.class);
		PollableChannel jmsInputChannel = applicationContext.getBean("jmsInputChannel", PollableChannel.class);
		input.send(new StringMessage("hello"));
		Message<String> message = (Message<String>) jmsInputChannel.receive(50000);
		System.out.println(message);
		Iterator<MessageHistoryEvent> historyIterator = message.getHeaders().getHistory().iterator();
		MessageHistoryEvent event = historyIterator.next();
		assertEquals("channel", event.getType());
		assertEquals("outbound-channel", event.getName());
		event = historyIterator.next();
		assertEquals("jms:outbound-channel-adapter", event.getType());
		assertEquals("jmsOutbound", event.getName());
		event = historyIterator.next();
		assertEquals("jms:inbound-channel-adapter", event.getType());
		assertEquals("sampleJmsInboundAdapter", event.getName());
		event = historyIterator.next();
		assertEquals("channel", event.getType());
		assertEquals("jmsInputChannel", event.getName());
	}

	@Test
	public void testWithHeaderMapperPropagatingOutboundHistoryWithGateways() throws Exception{
		ActiveMqTestUtils.prepare();
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext("MessageHistoryTests-gateways.xml", JmsMessageHistoryTests.class);
		SampleGateway gateway = applicationContext.getBean("sampleGateway", SampleGateway.class);
		SubscribableChannel inboundJmsChannel = applicationContext.getBean("inbound-jms-channel", SubscribableChannel.class);
		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message)
					throws MessageRejectedException, MessageHandlingException,
					MessageDeliveryException {
				Iterator<MessageHistoryEvent> historyIterator = message.getHeaders().getHistory().iterator();
				MessageHistoryEvent event = historyIterator.next();
				assertEquals("gateway", event.getType());
				assertEquals("sampleGateway", event.getName());
				event = historyIterator.next();
				assertEquals("publish-subscribe-channel", event.getType());
				assertEquals("channel-a", event.getName());
				event = historyIterator.next();
				assertEquals("jms:outbound-gateway", event.getType());
				assertEquals("jmsOutbound", event.getName());
				event = historyIterator.next();
				assertEquals("jms:inbound-gateway", event.getType());
				assertEquals("jmsInbound", event.getName());
				event = historyIterator.next();
				assertEquals("publish-subscribe-channel", event.getType());
				assertEquals("inbound-jms-channel", event.getName());
				
				MessageChannel channel = (MessageChannel) message.getHeaders().getReplyChannel();
				channel.send(new StringMessage("OK"));
			}
		};
		handler = Mockito.spy(handler);
		inboundJmsChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	public static interface SampleGateway{
		public void send(String value);
		public Message<?> echo(String value);
	}
	
	public static class SampleService{
		public Message<?> echoMessage(String value){
			System.out.println("IN SampleService");
			return new StringMessage(value);
		}
	}
	
	public static class SampleHeaderMapper extends DefaultJmsHeaderMapper {
		
		
		public void fromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage){
			super.fromHeaders(headers, jmsMessage);
			String messageHistory = headers.getHistory().toString();
			try {
				jmsMessage.setStringProperty("outbound_history", messageHistory);
			} catch (Exception e) {
				throw new MessagingException("Problem setting JMS properties", e);
			}
		}
		
		public Map<String, Object> toHeaders(javax.jms.Message jmsMessage){
			Map<String, Object> headers =  super.toHeaders(jmsMessage);
			MessageHistory history = new MessageHistory();
			String outboundHistory = (String) headers.get("outbound_history");
			StringTokenizer tok = new StringTokenizer(outboundHistory, ",[] ");
			while (tok.hasMoreTokens()) {
				String historyItem = tok.nextToken();
				String[] parsedHistory = StringUtils.split(historyItem, "#");
				String type = null;
				String name = historyItem;
				if (parsedHistory != null){
					name = parsedHistory[1];
					type = parsedHistory[0];
				}
				history.addEvent(new SampleComponent(name, type));
			}
			headers.put(MessageHeaders.HISTORY, history);
			headers.remove("outbound_history");
			return headers;
		}
	}
	
	public static class SampleComponent implements NamedComponent{
		private String name;
		private String type;
		public SampleComponent(String name, String type){
			this.name = name;
			this.type = type;
		}
		public String getComponentName() {
			return name;
		}
		public String getComponentType() {
			return type;
		}	
	}
}
