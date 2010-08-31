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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.GenericMessage;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class JmsMessageHistoryTests {

	@Test
	public void testInboundAdapter() throws Exception{
		ActiveMqTestUtils.prepare();
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext("MessageHistoryTests-context.xml", JmsMessageHistoryTests.class);
		SampleGateway gateway = applicationContext.getBean("sampleGateway", SampleGateway.class);
		PollableChannel jmsInputChannel = applicationContext.getBean("jmsInputChannel", PollableChannel.class);
		gateway.send("hello");
		Message<?> message = jmsInputChannel.receive(5000);
		Iterator<Properties> historyIterator = message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();
		Properties event = historyIterator.next();
		assertEquals("jms:inbound-channel-adapter", event.getProperty(MessageHistory.TYPE_PROPERTY));
		assertEquals("sampleJmsInboundAdapter", event.getProperty(MessageHistory.NAME_PROPERTY));
		event = historyIterator.next();
		assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));
		assertEquals("jmsInputChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
	}

	@Test @Ignore
	public void testWithHeaderMapperPropagatingOutboundHistory() throws Exception{
		ActiveMqTestUtils.prepare();
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext("MessageHistoryTests-withHeaderMapper.xml", JmsMessageHistoryTests.class);
		DirectChannel input = applicationContext.getBean("outbound-channel", DirectChannel.class);
		PollableChannel jmsInputChannel = applicationContext.getBean("jmsInputChannel", PollableChannel.class);
		input.send(new GenericMessage<String>("hello"));
		Message<?> message = jmsInputChannel.receive(50000);
		Iterator<Properties> historyIterator = message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();
		Properties event = historyIterator.next();
		assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));
		assertEquals("outbound-channel", event.getProperty(MessageHistory.NAME_PROPERTY));
		event = historyIterator.next();
		assertEquals("jms:outbound-channel-adapter", event.getProperty(MessageHistory.TYPE_PROPERTY));
		assertEquals("jmsOutbound", event.getProperty(MessageHistory.NAME_PROPERTY));
		event = historyIterator.next();
		assertEquals("jms:inbound-channel-adapter", event.getProperty(MessageHistory.TYPE_PROPERTY));
		assertEquals("sampleJmsInboundAdapter", event.getProperty(MessageHistory.NAME_PROPERTY));
		event = historyIterator.next();
		assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));
		assertEquals("jmsInputChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
	}

	@Test @Ignore
	public void testWithHeaderMapperPropagatingOutboundHistoryWithGateways() throws Exception{
		ActiveMqTestUtils.prepare();
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext("MessageHistoryTests-gateways.xml", JmsMessageHistoryTests.class);
		SampleGateway gateway = applicationContext.getBean("sampleGateway", SampleGateway.class);
		SubscribableChannel inboundJmsChannel = applicationContext.getBean("inbound-jms-channel", SubscribableChannel.class);
		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) {
				Iterator<Properties> historyIterator = message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();
				Properties event = historyIterator.next();
				assertEquals("gateway", event.getProperty(MessageHistory.TYPE_PROPERTY));
				assertEquals("sampleGateway", event.getProperty(MessageHistory.NAME_PROPERTY));
				event = historyIterator.next();
				assertEquals("publish-subscribe-channel", event.getProperty(MessageHistory.TYPE_PROPERTY));
				assertEquals("channel-a", event.getProperty(MessageHistory.NAME_PROPERTY));
				event = historyIterator.next();
				assertEquals("jms:outbound-gateway", event.getProperty(MessageHistory.TYPE_PROPERTY));
				assertEquals("jmsOutbound", event.getProperty(MessageHistory.NAME_PROPERTY));
				event = historyIterator.next();
				assertEquals("jms:inbound-gateway", event.getProperty(MessageHistory.TYPE_PROPERTY));
				assertEquals("jmsInbound", event.getProperty(MessageHistory.NAME_PROPERTY));
				event = historyIterator.next();
				assertEquals("publish-subscribe-channel", event.getProperty(MessageHistory.TYPE_PROPERTY));
				assertEquals("inbound-jms-channel", event.getProperty(MessageHistory.NAME_PROPERTY));
				
				MessageChannel channel = (MessageChannel) message.getHeaders().getReplyChannel();
				channel.send(new GenericMessage<String>("OK"));
			}
		};
		handler = Mockito.spy(handler);
		inboundJmsChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
	}


	public static interface SampleGateway {

		public void send(String value);

		public Message<?> echo(String value);

	}


	public static class SampleService {

		public Message<?> echoMessage(String value) {
			return new GenericMessage<String>(value);
		}

	}


	public static class SampleHeaderMapper extends DefaultJmsHeaderMapper {

		public void fromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
			super.fromHeaders(headers, jmsMessage);
			String messageHistory = headers.get(MessageHistory.HEADER_NAME, MessageHistory.class).toString();
			try {
				jmsMessage.setStringProperty("outbound_history", messageHistory);
			}
			catch (Exception e) {
				throw new MessagingException("Problem setting JMS properties", e);
			}
		}

		public Map<String, Object> toHeaders(javax.jms.Message jmsMessage) {
			Map<String, Object> headers =  super.toHeaders(jmsMessage);
			List<Properties> history = new ArrayList<Properties>();
			String outboundHistory = (String) headers.get("outbound_history");
			StringTokenizer outerTok = new StringTokenizer(outboundHistory, "[]");
			while (outerTok.hasMoreTokens()) {
				String historyItem = outerTok.nextToken();
				StringTokenizer innerTok = new StringTokenizer(historyItem, ",{} ");
				Properties historyEvent = new Properties();
				while (innerTok.hasMoreTokens()) {
					String prop = innerTok.nextToken();
					String[] keyAndValue = prop.split("=");
					historyEvent.setProperty(keyAndValue[0], keyAndValue[1]);
				}
				history.add(historyEvent);
			}
			headers.put(MessageHistory.HEADER_NAME, history);
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
