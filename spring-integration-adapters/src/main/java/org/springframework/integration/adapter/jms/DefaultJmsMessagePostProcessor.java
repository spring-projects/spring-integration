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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.springframework.integration.message.MessageHeader;

/**
 * A {@link JmsMessagePostProcessor} that passes attributes from a
 * {@link MessageHeader} to a JMS message before it is sent to its destination.
 * 
 * @author Mark Fisher
 */
public class DefaultJmsMessagePostProcessor implements JmsMessagePostProcessor {

	public void postProcessJmsMessage(Message jmsMessage, MessageHeader header) throws JMSException {
		Object jmsCorrelationId = header.getAttribute(JmsTargetAdapter.JMS_CORRELATION_ID);
		if (jmsCorrelationId != null && (jmsCorrelationId instanceof String)) {
			jmsMessage.setJMSCorrelationID((String) jmsCorrelationId);
		}
		Object jmsReplyTo = header.getAttribute(JmsTargetAdapter.JMS_REPLY_TO);
		if (jmsReplyTo != null && (jmsReplyTo instanceof Destination)) {
			jmsMessage.setJMSReplyTo((Destination) jmsReplyTo);
		}
		Object jmsType = header.getAttribute(JmsTargetAdapter.JMS_TYPE);
		if (jmsType != null && (jmsType instanceof String)) {
			jmsMessage.setJMSType((String) jmsType);
		}
	}

}
