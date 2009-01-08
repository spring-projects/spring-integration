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

package org.springframework.integration.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;

import org.junit.Test;

import org.springframework.integration.core.Message;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaders;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class DefaultJmsHeaderMapperTests {

	@Test
	public void testJmsReplyToMappedFromHeader() throws JMSException {
		Destination replyTo = new Destination() {};
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.REPLY_TO, replyTo).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertNotNull(jmsMessage.getJMSReplyTo());
		assertSame(replyTo, jmsMessage.getJMSReplyTo());
	}

	@Test
	public void testJmsReplyToIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.REPLY_TO, "not-a-destination").build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertNull(jmsMessage.getJMSReplyTo());
	}

	@Test
	public void testJmsCorrelationIdMappedFromHeader() throws JMSException {
		String jmsCorrelationId = "ABC-123";
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.CORRELATION_ID, jmsCorrelationId).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertNotNull(jmsMessage.getJMSCorrelationID());
		assertEquals(jmsCorrelationId, jmsMessage.getJMSCorrelationID());
	}

	@Test
	public void testJmsCorrelationIdIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.CORRELATION_ID, new Integer(123)).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertNull(jmsMessage.getJMSCorrelationID());
	}

	@Test
	public void testJmsTypeMappedFromHeader() throws JMSException {
		String jmsType = "testing";
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.TYPE, jmsType).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertNotNull(jmsMessage.getJMSType());
		assertEquals(jmsType, jmsMessage.getJMSType());
	}

	@Test
	public void testJmsTypeIgnoredIfIncorrectType() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader(JmsHeaders.TYPE, new Integer(123)).build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		assertNull(jmsMessage.getJMSType());
	}

	@Test
	public void testUserDefinedPropertyMappedFromHeader() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("foo", new Integer(123))
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("foo");
		assertNotNull(value);
		assertEquals(Integer.class, value.getClass());
		assertEquals(123, ((Integer) value).intValue());
	}

	@Test
	public void testUserDefinedPropertyWithUnsupportedType() throws JMSException {
		Destination destination = new Destination() {};
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("destination", destination)
				.build();
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.fromHeaders(message.getHeaders(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("destination");
		assertNull(value);
	}

	@Test
	public void testJmsReplyToMappedToHeader() throws JMSException {
		Destination replyTo = new Destination() {};
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSReplyTo(replyTo);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.REPLY_TO);
		assertNotNull(attrib);
		assertSame(replyTo, attrib);
	}

	@Test
	public void testJmsMessageIdMappedToHeader() throws JMSException {
		String messageId = "ID:ABC-123";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSMessageID(messageId);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.MESSAGE_ID);
		assertNotNull(attrib);
		assertSame(messageId, attrib);
	}

	@Test
	public void testJmsCorrelationIdMappedToHeader() throws JMSException {
		String correlationId = "ABC-123";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSCorrelationID(correlationId);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.CORRELATION_ID);
		assertNotNull(attrib);
		assertSame(correlationId, attrib);
	}

	@Test
	public void testJmsTypeMappedToHeader() throws JMSException {
		String type = "testing";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSType(type);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get(JmsHeaders.TYPE);
		assertNotNull(attrib);
		assertSame(type, attrib);
	}

	@Test
	public void testUserDefinedPropertyMappedToHeader() throws JMSException {
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setIntProperty("foo", 123);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		Map<String, Object> headers = mapper.toHeaders(jmsMessage);
		Object attrib = headers.get("foo");
		assertNotNull(attrib);
		assertEquals(Integer.class, attrib.getClass());
		assertEquals(123, ((Integer) attrib).intValue());
	}

	@Test
	public void testPropertyMappingExceptionIsNotFatal() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("foo", new Integer(123))
				.setHeader("bad", new Integer(456))
				.setHeader("bar", new Integer(789))
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
		assertNotNull(foo);
		Object bar = jmsMessage.getObjectProperty("bar");
		assertNotNull(bar);
		Object bad = jmsMessage.getObjectProperty("bad");
		assertNull(bad);
	}

	@Test
	public void testIllegalArgumentExceptionIsNotFatal() throws JMSException {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("foo", new Integer(123))
				.setHeader("bad", new Integer(456))
				.setHeader("bar", new Integer(789))
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
		assertNotNull(foo);
		Object bar = jmsMessage.getObjectProperty("bar");
		assertNotNull(bar);
		Object bad = jmsMessage.getObjectProperty("bad");
		assertNull(bad);
	}

}
