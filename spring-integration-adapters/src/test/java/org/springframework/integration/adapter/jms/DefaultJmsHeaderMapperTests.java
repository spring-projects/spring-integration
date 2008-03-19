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

package org.springframework.integration.adapter.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.jms.Destination;
import javax.jms.JMSException;

import org.junit.Test;

import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class DefaultJmsHeaderMapperTests {

	@Test
	public void testJmsReplyToMappedFromHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		Destination replyTo = new Destination() {};
		message.getHeader().setAttribute(JmsAttributeKeys.REPLY_TO, replyTo);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		assertNotNull(jmsMessage.getJMSReplyTo());
		assertSame(replyTo, jmsMessage.getJMSReplyTo());
	}

	@Test
	public void testJmsReplyToIgnoredIfIncorrectType() throws JMSException {
		StringMessage message = new StringMessage("test");
		message.getHeader().setAttribute(JmsAttributeKeys.REPLY_TO, "not-a-destination");
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		assertNull(jmsMessage.getJMSReplyTo());
	}

	@Test
	public void testJmsCorrelationIdMappedFromHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		String jmsCorrelationId = "ABC-123";
		message.getHeader().setAttribute(JmsAttributeKeys.CORRELATION_ID, jmsCorrelationId);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		assertNotNull(jmsMessage.getJMSCorrelationID());
		assertEquals(jmsCorrelationId, jmsMessage.getJMSCorrelationID());
	}

	@Test
	public void testJmsCorrelationIdIgnoredIfIncorrectType() throws JMSException {
		StringMessage message = new StringMessage("test");
		message.getHeader().setAttribute(JmsAttributeKeys.CORRELATION_ID, new Integer(123));
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		assertNull(jmsMessage.getJMSCorrelationID());
	}

	@Test
	public void testJmsTypeMappedFromHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		String jmsType = "testing";
		message.getHeader().setAttribute(JmsAttributeKeys.TYPE, jmsType);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		assertNotNull(jmsMessage.getJMSType());
		assertEquals(jmsType, jmsMessage.getJMSType());
	}

	@Test
	public void testJmsTypeIgnoredIfIncorrectType() throws JMSException {
		StringMessage message = new StringMessage("test");
		message.getHeader().setAttribute(JmsAttributeKeys.TYPE, new Integer(123));
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		assertNull(jmsMessage.getJMSType());
	}

	@Test
	public void testUserDefinedPropertyMappedFromHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		message.getHeader().setAttribute(JmsAttributeKeys.USER_DEFINED_ATTRIBUTE_PREFIX + "foo", new Integer(123));
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("foo");
		assertNotNull(value);
		assertEquals(Integer.class, value.getClass());
		assertEquals(123, ((Integer) value).intValue());
	}

	@Test
	public void testUserDefinedPropertyWithUnsupportedType() throws JMSException {
		StringMessage message = new StringMessage("test");
		Destination destination = new Destination() {};
		message.getHeader().setAttribute(JmsAttributeKeys.USER_DEFINED_ATTRIBUTE_PREFIX + "destination", destination);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		javax.jms.Message jmsMessage = new StubTextMessage();
		mapper.mapFromMessageHeader(message.getHeader(), jmsMessage);
		Object value = jmsMessage.getObjectProperty("foo");
		assertNull(value);
	}

	@Test
	public void testJmsReplyToMappedToHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		Destination replyTo = new Destination() {};
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSReplyTo(replyTo);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		mapper.mapToMessageHeader(jmsMessage, message.getHeader());
		Object attrib = message.getHeader().getAttribute(JmsAttributeKeys.REPLY_TO);
		assertNotNull(attrib);
		assertSame(replyTo, attrib);
	}

	@Test
	public void testJmsCorrelationIdMappedToHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		String correlationId = "ABC-123";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSCorrelationID(correlationId);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		mapper.mapToMessageHeader(jmsMessage, message.getHeader());
		Object attrib = message.getHeader().getAttribute(JmsAttributeKeys.CORRELATION_ID);
		assertNotNull(attrib);
		assertSame(correlationId, attrib);
	}

	@Test
	public void testJmsTypeMappedToHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		String type = "testing";
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setJMSType(type);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		mapper.mapToMessageHeader(jmsMessage, message.getHeader());
		Object attrib = message.getHeader().getAttribute(JmsAttributeKeys.TYPE);
		assertNotNull(attrib);
		assertSame(type, attrib);
	}

	@Test
	public void testUserDefinedPropertyMappedToHeader() throws JMSException {
		StringMessage message = new StringMessage("test");
		javax.jms.Message jmsMessage = new StubTextMessage();
		jmsMessage.setIntProperty("foo", 123);
		DefaultJmsHeaderMapper mapper = new DefaultJmsHeaderMapper();
		mapper.mapToMessageHeader(jmsMessage, message.getHeader());
		Object attrib = message.getHeader().getAttribute(JmsAttributeKeys.USER_DEFINED_ATTRIBUTE_PREFIX + "foo");
		assertNotNull(attrib);
		assertEquals(Integer.class, attrib.getClass());
		assertEquals(123, ((Integer) attrib).intValue());
	}

}
