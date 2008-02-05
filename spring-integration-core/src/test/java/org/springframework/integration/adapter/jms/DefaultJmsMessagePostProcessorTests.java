/*
 * Copyright 2002-2007 the original author or authors.
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
public class DefaultJmsMessagePostProcessorTests {

	@Test
	public void testJmsReplyTo() throws JMSException {
		StringMessage message = new StringMessage("test1");
		Destination replyTo = new Destination() {};
		message.getHeader().setAttribute(JmsTargetAdapter.JMS_REPLY_TO, replyTo);
		DefaultJmsMessagePostProcessor postProcessor = new DefaultJmsMessagePostProcessor();
		javax.jms.Message jmsMessage = new StubTextMessage();
		postProcessor.postProcessJmsMessage(jmsMessage, message.getHeader());
		assertNotNull(jmsMessage.getJMSReplyTo());
		assertSame(replyTo, jmsMessage.getJMSReplyTo());
	}

	@Test
	public void testJmsReplyToIgnoredIfIncorrectType() throws JMSException {
		StringMessage message = new StringMessage("test1");
		message.getHeader().setAttribute(JmsTargetAdapter.JMS_REPLY_TO, "not-a-destination");
		DefaultJmsMessagePostProcessor postProcessor = new DefaultJmsMessagePostProcessor();
		javax.jms.Message jmsMessage = new StubTextMessage();
		postProcessor.postProcessJmsMessage(jmsMessage, message.getHeader());
		assertNull(jmsMessage.getJMSReplyTo());
	}

	@Test
	public void testJmsCorrelationId() throws JMSException {
		StringMessage message = new StringMessage("test1");
		String jmsCorrelationId = "ABC-123";
		message.getHeader().setAttribute(JmsTargetAdapter.JMS_CORRELATION_ID, jmsCorrelationId);
		DefaultJmsMessagePostProcessor postProcessor = new DefaultJmsMessagePostProcessor();
		javax.jms.Message jmsMessage = new StubTextMessage();
		postProcessor.postProcessJmsMessage(jmsMessage, message.getHeader());
		assertNotNull(jmsMessage.getJMSCorrelationID());
		assertEquals(jmsCorrelationId, jmsMessage.getJMSCorrelationID());
	}

	@Test
	public void testJmsCorrelationIdIgnoredIfIncorrectType() throws JMSException {
		StringMessage message = new StringMessage("test1");
		message.getHeader().setAttribute(JmsTargetAdapter.JMS_CORRELATION_ID, new Integer(123));
		DefaultJmsMessagePostProcessor postProcessor = new DefaultJmsMessagePostProcessor();
		javax.jms.Message jmsMessage = new StubTextMessage();
		postProcessor.postProcessJmsMessage(jmsMessage, message.getHeader());
		assertNull(jmsMessage.getJMSCorrelationID());
	}

	@Test
	public void testJmsType() throws JMSException {
		StringMessage message = new StringMessage("test1");
		String jmsType = "testing";
		message.getHeader().setAttribute(JmsTargetAdapter.JMS_TYPE, jmsType);
		DefaultJmsMessagePostProcessor postProcessor = new DefaultJmsMessagePostProcessor();
		javax.jms.Message jmsMessage = new StubTextMessage();
		postProcessor.postProcessJmsMessage(jmsMessage, message.getHeader());
		assertNotNull(jmsMessage.getJMSType());
		assertEquals(jmsType, jmsMessage.getJMSType());
	}

	@Test
	public void testJmsTypeIgnoredIfIncorrectType() throws JMSException {
		StringMessage message = new StringMessage("test1");
		message.getHeader().setAttribute(JmsTargetAdapter.JMS_TYPE, new Integer(123));
		DefaultJmsMessagePostProcessor postProcessor = new DefaultJmsMessagePostProcessor();
		javax.jms.Message jmsMessage = new StubTextMessage();
		postProcessor.postProcessJmsMessage(jmsMessage, message.getHeader());
		assertNull(jmsMessage.getJMSType());
	}

}
