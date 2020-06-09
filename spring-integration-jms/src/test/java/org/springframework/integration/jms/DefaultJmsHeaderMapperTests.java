/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class DefaultJmsHeaderMapperTests {

	@Test
	public void testJmsReplyToMappedFromHeader() throws JMSException {
		Destination replyTo = new Destination() {

		};
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.REPLY_TO, replyTo).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSReplyTo()).isNotNull();
		assertThat(jmsMessage.getJMSReplyTo()).isSameAs(replyTo);
	}

	@Test
	public void testJmsReplyToIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.REPLY_TO, "not-a-destination").build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSReplyTo()).isNull();
	}

	@Test
	public void testJmsCorrelationIdMappedFromHeader() throws JMSException {
		String jmsCorrelationId = "ABC-123";
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.CORRELATION_ID, jmsCorrelationId).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNotNull();
		assertThat(jmsMessage.getJMSCorrelationID()).isEqualTo(jmsCorrelationId);
	}

	@Test
	public void testJmsCorrelationIdNumberConvertsToString() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.CORRELATION_ID, 123).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isEqualTo("123");
	}

	@Test
	public void testJmsCorrelationIdIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.CORRELATION_ID, new Date()).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNull();
	}

	@Test
	public void testJmsTypeMappedFromHeader() throws JMSException {
		String jmsType = "testing";
		Message<String> message =
				MessageBuilder.withPayload("test")
						.setHeader(JmsHeaders.TYPE, jmsType)
						.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSType()).isNotNull();
		assertThat(jmsMessage.getJMSType()).isEqualTo(jmsType);
	}

	@Test
	public void testJmsTypeIgnoredIfIncorrectType() throws JMSException {
		Message<String> message =
				MessageBuilder.withPayload("test")
						.setHeader(JmsHeaders.TYPE, 123)
						.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSType()).isNull();
	}

	@Test
	public void testContentTypePropertyMappedFromHeader() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(MessageHeaders.CONTENT_TYPE, "foo")
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("content_type");
		assertThat(value).isNotNull();
		assertThat(value).isEqualTo("foo");
	}

	@Test
	public void testUserDefinedPropertyMappedFromHeader() throws JMSException {
		Message<String> message =
				MessageBuilder.withPayload("test")
						.setHeader("foo", 123)
						.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("foo");
		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(Integer.class);
		assertThat(((Integer) value).intValue()).isEqualTo(123);
	}

	@Test
	public void testUserDefinedPropertyMappedFromHeaderWithCustomPrefix() throws JMSException {
		Message<String> message =
				MessageBuilder.withPayload("test")
						.setHeader("foo", 123)
						.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		mapper.setOutboundPrefix("custom_");
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("custom_foo");
		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(Integer.class);
		assertThat(((Integer) value).intValue()).isEqualTo(123);
	}

	@Test
	public void testUserDefinedPropertyWithUnsupportedType() throws JMSException {
		Destination destination = new Destination() {

		};
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("destination", destination)
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("destination");
		assertThat(value).isNull();
	}

	@Test
	public void testJmsReplyToMappedToHeader() throws JMSException {
		Destination replyTo = new Destination() {

		};
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSReplyTo(replyTo);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.REPLY_TO);
		assertThat(attrib).isNotNull();
		assertThat(attrib).isSameAs(replyTo);
	}

	@Test
	public void testJmsMessageIdMappedToHeader() throws JMSException {
		String messageId = "ID:ABC-123";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSMessageID(messageId);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.MESSAGE_ID);
		assertThat(attrib).isNotNull();
		assertThat(attrib).isSameAs(messageId);
	}

	@Test
	public void testJmsCorrelationIdMappedToHeader() throws JMSException {
		String correlationId = "ABC-123";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSCorrelationID(correlationId);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.CORRELATION_ID);
		assertThat(attrib).isNotNull();
		assertThat(attrib).isSameAs(correlationId);
	}

	@Test
	public void testJmsTypeMappedToHeader() throws JMSException {
		String type = "testing";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSType(type);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.TYPE);
		assertThat(attrib).isNotNull();
		assertThat(attrib).isSameAs(type);
	}

	@Test
	public void testJmsTimestampMappedToHeader() throws JMSException {
		long timestamp = 123L;
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSTimestamp(timestamp);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.TIMESTAMP);
		assertThat(attrib).isNotNull();
		assertThat(attrib).isEqualTo(timestamp);
	}

	@Test
	public void testJmsPriorityMappedToHeader() throws JMSException {
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSPriority(5);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(IntegrationMessageHeaderAccessor.PRIORITY);
		assertThat(attrib).isNotNull();
		assertThat(attrib).isEqualTo(5);
	}

	@Test
	public void testJmsPriorityNotMappedToHeader() throws JMSException {
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSPriority(5);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		mapper.setMapInboundPriority(false);
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(IntegrationMessageHeaderAccessor.PRIORITY);
		assertThat(attrib).isNull();
	}

	@Test
	public void testContentTypePropertyMappedToHeader() throws JMSException {
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setStringProperty("content_type", "foo");
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(MessageHeaders.CONTENT_TYPE);
		assertThat(attrib).isNotNull();
		assertThat(attrib).isEqualTo("foo");
	}

	@Test
	public void testUserDefinedPropertyMappedToHeader() throws JMSException {
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setIntProperty("foo", 123);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get("foo");
		assertThat(attrib).isNotNull();
		assertThat(attrib.getClass()).isEqualTo(Integer.class);
		assertThat(((Integer) attrib).intValue()).isEqualTo(123);
	}

	@Test
	public void testUserDefinedPropertyMappedToHeaderWithCustomPrefix() throws JMSException {
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setIntProperty("foo", 123);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		mapper.setInboundPrefix("custom_");
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object header = headers.get("custom_foo");
		assertThat(header).isNotNull();
		assertThat(header.getClass()).isEqualTo(Integer.class);
		assertThat(((Integer) header).intValue()).isEqualTo(123);
	}

	@Test
	public void testPropertyMappingExceptionIsNotFatal() throws JMSException {
		Message<String> message =
				MessageBuilder.withPayload("test")
						.setHeader("foo", 123)
						.setHeader("bad", 456)
						.setHeader("bar", 789)
						.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public void setObjectProperty(String name, Object value) throws JMSException {
				if (name.equals("bad")) {
					throw new JMSException("illegal property");
				}
				super.setObjectProperty(name, value);
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object foo = jmsMessage.getObjectProperty("foo");
		assertThat(foo).isNotNull();
		Object bar = jmsMessage.getObjectProperty("bar");
		assertThat(bar).isNotNull();
		Object bad = jmsMessage.getObjectProperty("bad");
		assertThat(bad).isNull();
	}

	@Test
	public void testIllegalArgumentExceptionIsNotFatal() throws JMSException {
		Message<String> message =
				MessageBuilder.withPayload("test")
						.setHeader("foo", 123)
						.setHeader("bad", 456)
						.setHeader("bar", 789)
						.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public void setObjectProperty(String name, Object value) throws JMSException {
				if (name.equals("bad")) {
					throw new IllegalArgumentException("illegal property");
				}
				super.setObjectProperty(name, value);
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object foo = jmsMessage.getObjectProperty("foo");
		assertThat(foo).isNotNull();
		Object bar = jmsMessage.getObjectProperty("bar");
		assertThat(bar).isNotNull();
		Object bad = jmsMessage.getObjectProperty("bad");
		assertThat(bad).isNull();
	}

	@Test
	public void attemptToWriteDisallowedReplyToPropertyIsNotFatal() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.REPLY_TO, new StubDestination())
				.setHeader("foo", "bar")
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public void setJMSReplyTo(Destination replyTo) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSReplyTo()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToWriteDisallowedTypePropertyIsNotFatal() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.TYPE, "someType")
				.setHeader("foo", "bar")
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public void setJMSType(String type) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSType()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToWriteDisallowedCorrelationIdStringPropertyIsNotFatal() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.CORRELATION_ID, "abc")
				.setHeader("foo", "bar")
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public void setJMSCorrelationID(String correlationId) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToWriteDisallowedCorrelationIdNumberPropertyIsNotFatal() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.CORRELATION_ID, 123)
				.setHeader("foo", "bar")
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public void setJMSCorrelationID(String correlationId) throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertThat(jmsMessage.getJMSCorrelationID()).isNull();
		assertThat(jmsMessage.getStringProperty("foo")).isNotNull();
		assertThat(jmsMessage.getStringProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToReadDisallowedMessageIdPropertyIsNotFatal() throws JMSException {
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public String getJMSMessageID() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		jmsMessage.setStringProperty("foo", "bar");
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		assertThat(headers.get(JmsHeaders.MESSAGE_ID)).isNull();
		assertThat(headers.get("foo")).isNotNull();
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToReadDisallowedCorrelationIdPropertyIsNotFatal() throws JMSException {
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public String getJMSCorrelationID() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		jmsMessage.setStringProperty("foo", "bar");
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		assertThat(headers.get(JmsHeaders.CORRELATION_ID)).isNull();
		assertThat(headers.get("foo")).isNotNull();
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToReadDisallowedTypePropertyIsNotFatal() throws JMSException {
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public String getJMSType() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		jmsMessage.setStringProperty("foo", "bar");
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		assertThat(headers.get(JmsHeaders.TYPE)).isNull();
		assertThat(headers.get("foo")).isNotNull();
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToReadDisallowedReplyToPropertyIsNotFatal() throws JMSException {
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public Destination getJMSReplyTo() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		jmsMessage.setStringProperty("foo", "bar");
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		assertThat(headers.get(JmsHeaders.REPLY_TO)).isNull();
		assertThat(headers.get("foo")).isNotNull();
		assertThat(headers.get("foo")).isEqualTo("bar");
	}

	@Test
	public void attemptToReadDisallowedRedeliveredPropertyIsNotFatal() throws JMSException {
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage() {

			@Override
			public boolean getJMSRedelivered() throws JMSException {
				throw new JMSException("illegal property");
			}
		};
		jmsMessage.setStringProperty("foo", "bar");
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		assertThat(headers.get(JmsHeaders.REDELIVERED)).isNull();
		assertThat(headers.get("foo")).isNotNull();
		assertThat(headers.get("foo")).isEqualTo("bar");
	}


	@Test
	public void testJsonHeaderMapping() throws JMSException {

		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setTargetType(MessageType.TEXT);
		converter.setTypeIdPropertyName("javatype");

		Session session = Mockito.mock(Session.class);

		Mockito.doAnswer(invocation -> new StubTextMessage(invocation.getArgument(0))).when(session)
				.createTextMessage(Mockito.anyString());

		javax.jms.Message request = converter.toMessage(new Foo(), session);

		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(request);

		javax.jms.Message reply = converter.toMessage("foo", session);
		mapper.fromHeaders(new MessageHeaders(headers), reply);

		Object result = converter.fromMessage(reply);
		assertThat(result).isInstanceOf(String.class);
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "javatype")
	private static class Foo {

	}

}
