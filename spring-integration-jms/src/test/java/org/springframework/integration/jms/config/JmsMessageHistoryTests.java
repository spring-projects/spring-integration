/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsMessageHistoryTests extends ActiveMQMultiContextTests {

	@Autowired
	SampleGateway sampleGateway;

	@Autowired
	PollableChannel jmsInputChannel;

	@Test
	public void testInboundAdapter() {
		sampleGateway.send("hello");
		Message<?> message = this.jmsInputChannel.receive(5000);
		Iterator<Properties> historyIterator = message.getHeaders()
				.get(MessageHistory.HEADER_NAME, MessageHistory.class)
				.iterator();
		Properties event1 = historyIterator.next();
		assertThat(event1.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("jms:inbound-channel-adapter");
		assertThat(event1.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("sampleJmsInboundAdapter");
		Properties event2 = historyIterator.next();
		assertThat(event2.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");
		assertThat(event2.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("jmsInputChannel");
	}

	public interface SampleGateway {

		void send(String value);

		Message<?> echo(String value);

	}

	public static class SampleService {

		public Message<?> echoMessage(String value) {
			return new GenericMessage<String>(value);
		}

	}

	public static class SampleHeaderMapper extends DefaultJmsHeaderMapper {

		public void fromHeaders(MessageHeaders headers, jakarta.jms.Message jmsMessage) {
			super.fromHeaders(headers, jmsMessage);
			String messageHistory = headers.get(MessageHistory.HEADER_NAME, MessageHistory.class).toString();
			try {
				jmsMessage.setStringProperty("outbound_history", messageHistory);
			}
			catch (Exception e) {
				throw new MessagingException("Problem setting JMS properties", e);
			}
		}

		public Map<String, Object> toHeaders(jakarta.jms.Message jmsMessage) {
			Map<String, Object> headers = super.toHeaders(jmsMessage);
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

	public static class SampleComponent implements NamedComponent {

		private String name;

		private String type;

		public SampleComponent(String name, String type) {
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
