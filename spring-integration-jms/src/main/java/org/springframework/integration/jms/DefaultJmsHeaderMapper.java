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

package org.springframework.integration.jms;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Destination;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.MessageMappingException;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link JmsHeaderMapper}.
 * <p/>
 * This implementation copies JMS API headers (e.g. JMSReplyTo) to and from
 * Spring Integration Messages. Any user-defined properties will also be copied
 * from a JMS Message to a Spring Integration Message, and any other headers
 * on a Spring Integration Message (beyond the JMS API headers) will likewise
 * be copied to a JMS Message. Those other headers will be copied to the
 * general properties of a JMS Message whereas the JMS API headers are passed
 * to the appropriate setter methods (e.g. setJMSReplyTo).
 * <p/>
 * Constants for the JMS API headers are defined in {@link JmsHeaders}.
 * Note that the JMSMessageID and JMSRedelivered flag are only copied
 * <em>from</em> a JMS Message. Those values will <em>not</em> be passed
 * along from a Spring Integration Message to an outbound JMS Message.
 * 
 * @author Mark Fisher
 */
public class DefaultJmsHeaderMapper implements JmsHeaderMapper {

	private static List<Class<?>> SUPPORTED_PROPERTY_TYPES = Arrays.asList(new Class<?>[] {
			Boolean.class, Byte.class, Double.class, Float.class, Integer.class, Long.class, Short.class, String.class });


	private final Log logger = LogFactory.getLog(this.getClass());


	public void fromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
		try {
			Object jmsCorrelationId = headers.get(JmsHeaders.CORRELATION_ID);
			if (jmsCorrelationId instanceof Number) {
				jmsCorrelationId = ((Number) jmsCorrelationId).toString();
			}
			if (jmsCorrelationId instanceof String) {
				jmsMessage.setJMSCorrelationID((String) jmsCorrelationId);
			}
			Object jmsReplyTo = headers.get(JmsHeaders.REPLY_TO);
			if (jmsReplyTo instanceof Destination) {
				jmsMessage.setJMSReplyTo((Destination) jmsReplyTo);
			}
			Object jmsType = headers.get(JmsHeaders.TYPE);
			if (jmsType instanceof String) {
				jmsMessage.setJMSType((String) jmsType);
			}
			Set<String> attributeNames = headers.keySet();
			for (String attributeName : attributeNames) {
				if (!attributeName.startsWith(JmsHeaders.PREFIX)) {
					if (StringUtils.hasText(attributeName)) {
						Object value = headers.get(attributeName);
						if (value != null && SUPPORTED_PROPERTY_TYPES.contains(value.getClass())) {
							try {
								jmsMessage.setObjectProperty(attributeName, value);
							}
							catch (Exception e) {
								if (attributeName.startsWith("JMSX")) {
									if (logger.isTraceEnabled()) {
										logger.trace("skipping reserved header, it cannot be set by client: "
												+ attributeName);
									}
								}
								else if (logger.isWarnEnabled()) {
									logger.warn("failed to map Message header '"
											+ attributeName + "' to JMS property", e);
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping properties from MessageHeaders", e);
			}
		}
	}

	public Map<String, Object> toHeaders(javax.jms.Message jmsMessage) {
		Map<String, Object> headers = new HashMap<String, Object>();
		try {
			String messageId = jmsMessage.getJMSMessageID();
			if (messageId != null) {
				headers.put(JmsHeaders.MESSAGE_ID, messageId);
			}
			String correlationId = jmsMessage.getJMSCorrelationID();
			if (correlationId != null) {
				headers.put(JmsHeaders.CORRELATION_ID, correlationId);
			}
			Destination replyTo = jmsMessage.getJMSReplyTo();
			if (replyTo != null) {
				headers.put(JmsHeaders.REPLY_TO, replyTo);
			}
			headers.put(JmsHeaders.REDELIVERED, jmsMessage.getJMSRedelivered());
			String type = jmsMessage.getJMSType();
			if (type != null) {
				headers.put(JmsHeaders.TYPE, type);
			}
			Enumeration<?> jmsPropertyNames = jmsMessage.getPropertyNames();
			if (jmsPropertyNames != null) {
				while (jmsPropertyNames.hasMoreElements()) {
					String propertyName = jmsPropertyNames.nextElement().toString();
					try {
						headers.put(propertyName, jmsMessage.getObjectProperty(propertyName));
					}
					catch (Exception e) {
						if (logger.isWarnEnabled()) {
							logger.warn("error occurred while mapping JMS property '"
									+ propertyName + "' to Message header", e);
						}
					}
				}
			}
		}
		catch (JMSException e) {
			throw new MessageMappingException("failure occurred while mapping JMS properties to MessageHeaders", e);
		}
		return headers;
	}

}
