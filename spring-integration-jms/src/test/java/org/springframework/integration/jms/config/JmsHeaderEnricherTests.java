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

import static org.junit.Assert.assertEquals;

import javax.jms.Destination;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.jms.DefaultJmsHeaderMapper;
import org.springframework.integration.jms.JmsHeaders;
import org.springframework.integration.jms.StubTextMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsHeaderEnricherTests {

	@Autowired
	private MessageChannel valueTestInput;

	@Autowired
	private MessageChannel expressionTestInput;

	@Autowired
	private PollableChannel output;

	@Autowired
	private Destination testDestination;

	@Test // INT-804
	public void verifyReplyToValue() throws Exception {
		valueTestInput.send(new GenericMessage<String>("test"));
		Message<?> result = output.receive(0);
		assertEquals(testDestination, result.getHeaders().get(JmsHeaders.REPLY_TO));
		StubTextMessage jmsMessage = new StubTextMessage();
		DefaultJmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
		headerMapper.fromHeaders(result.getHeaders(), jmsMessage);
		assertEquals(testDestination, jmsMessage.getJMSReplyTo());
	}

	@Test
	public void verifyCorrelationIdValue() throws Exception {
		valueTestInput.send(new GenericMessage<String>("test"));
		Message<?> result = output.receive(0);
		assertEquals("ABC", result.getHeaders().get(JmsHeaders.CORRELATION_ID));
		StubTextMessage jmsMessage = new StubTextMessage();
		DefaultJmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
		headerMapper.fromHeaders(result.getHeaders(), jmsMessage);
		assertEquals("ABC", jmsMessage.getJMSCorrelationID());
	}

	@Test // see INT-1122 and INT-1123
	public void verifyCorrelationIdExpression() throws Exception {
		expressionTestInput.send(new GenericMessage<String>("test"));
		Message<?> result = output.receive(0);
		assertEquals(123, result.getHeaders().get(JmsHeaders.CORRELATION_ID));
		StubTextMessage jmsMessage = new StubTextMessage();
		DefaultJmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
		headerMapper.fromHeaders(result.getHeaders(), jmsMessage);
		assertEquals("123", jmsMessage.getJMSCorrelationID());
	}

}
