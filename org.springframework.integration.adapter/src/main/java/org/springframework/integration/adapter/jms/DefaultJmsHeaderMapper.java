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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.adapter.MessageHeaderMapper;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.util.StringUtils;

/**
 * A {@link HeaderMapper} implementation for JMS {@link javax.jms.Message Messages}. 
 * 
 * @author Mark Fisher
 */
public class DefaultJmsHeaderMapper implements MessageHeaderMapper<javax.jms.Message> {

	private static List<Class<?>> SUPPORTED_PROPERTY_TYPES = Arrays.asList(new Class<?>[] {
			Boolean.class, Byte.class, Double.class, Float.class, Integer.class, Long.class, Short.class, String.class });


	private final Log logger = LogFactory.getLog(this.getClass());


	public void mapFromMessageHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
		try {
			Object jmsCorrelationId = headers.get(JmsAttributeKeys.CORRELATION_ID);
			if (jmsCorrelationId != null && (jmsCorrelationId instanceof String)) {
				jmsMessage.setJMSCorrelationID((String) jmsCorrelationId);
			}
			Object jmsReplyTo = headers.get(JmsAttributeKeys.REPLY_TO);
			if (jmsReplyTo != null && (jmsReplyTo instanceof Destination)) {
				jmsMessage.setJMSReplyTo((Destination) jmsReplyTo);
			}
			Object jmsType = headers.get(JmsAttributeKeys.TYPE);
			if (jmsType != null && (jmsType instanceof String)) {
				jmsMessage.setJMSType((String) jmsType);
			}
			String prefix = JmsAttributeKeys.USER_DEFINED_ATTRIBUTE_PREFIX;
			Set<String> attributeNames = headers.keySet();
			for (String attributeName : attributeNames) {
				if (attributeName.startsWith(prefix)) {
					String jmsAttributeName = attributeName.substring(prefix.length());
					if (StringUtils.hasText(attributeName)) {
						Object value = headers.get(attributeName);
						if (value != null && SUPPORTED_PROPERTY_TYPES.contains(value.getClass())) {
							try {
								jmsMessage.setObjectProperty(jmsAttributeName, value);
							}
							catch (Throwable t) {
								if (logger.isWarnEnabled()) {
									logger.warn("failed to map property '" + jmsAttributeName + "' from MessageHeader", t);
								}
							}
						}
					}
				}
			}
		}
		catch (Throwable t) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping properties from MessageHeader", t);
			}
		}
	}

	public Map<String, Object> mapToMessageHeaders(javax.jms.Message jmsMessage) {
		Map<String, Object> headers = new HashMap<String, Object>();
		try {
			String correlationId = jmsMessage.getJMSCorrelationID();
			if (correlationId != null) {
				headers.put(JmsAttributeKeys.CORRELATION_ID, correlationId);
			}
			Destination replyTo = jmsMessage.getJMSReplyTo();
			if (replyTo != null) {
				headers.put(JmsAttributeKeys.REPLY_TO, replyTo);
			}
			headers.put(JmsAttributeKeys.REDELIVERED, jmsMessage.getJMSRedelivered());
			String type = jmsMessage.getJMSType();
			if (type != null) {
				headers.put(JmsAttributeKeys.TYPE, type);
			}
			Enumeration<?> jmsPropertyNames = jmsMessage.getPropertyNames();
			if (jmsPropertyNames != null) {
				while (jmsPropertyNames.hasMoreElements()) {
					String propertyName = jmsPropertyNames.nextElement().toString();
					headers.put(JmsAttributeKeys.USER_DEFINED_ATTRIBUTE_PREFIX + propertyName,
							jmsMessage.getObjectProperty(propertyName));
				}
			}
		}
		catch (Throwable t) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping properties to MessageHeader", t);
			}
		}
		return headers;
	}

}
