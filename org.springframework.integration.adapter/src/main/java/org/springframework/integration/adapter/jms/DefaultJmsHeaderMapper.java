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
import java.util.List;
import java.util.Set;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.adapter.MessageHeaderMapper;
import org.springframework.integration.message.MessageHeader;
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


	public void mapFromMessageHeader(MessageHeader header, javax.jms.Message jmsMessage) {
		try {
			Object jmsCorrelationId = header.getAttribute(JmsAttributeKeys.CORRELATION_ID);
			if (jmsCorrelationId != null && (jmsCorrelationId instanceof String)) {
				jmsMessage.setJMSCorrelationID((String) jmsCorrelationId);
			}
			Object jmsReplyTo = header.getAttribute(JmsAttributeKeys.REPLY_TO);
			if (jmsReplyTo != null && (jmsReplyTo instanceof Destination)) {
				jmsMessage.setJMSReplyTo((Destination) jmsReplyTo);
			}
			Object jmsType = header.getAttribute(JmsAttributeKeys.TYPE);
			if (jmsType != null && (jmsType instanceof String)) {
				jmsMessage.setJMSType((String) jmsType);
			}
			String prefix = JmsAttributeKeys.USER_DEFINED_ATTRIBUTE_PREFIX;
			Set<String> attributeNames = header.getAttributeNames();
			for (String attributeName : attributeNames) {
				if (attributeName.startsWith(prefix)) {
					String jmsAttributeName = attributeName.substring(prefix.length());
					if (StringUtils.hasText(attributeName)) {
						Object value = header.getAttribute(attributeName);
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

	public void mapToMessageHeader(javax.jms.Message jmsMessage, MessageHeader header) {
		try {
			String correlationId = jmsMessage.getJMSCorrelationID();
			if (correlationId != null) {
				header.setAttribute(JmsAttributeKeys.CORRELATION_ID, correlationId);
			}
			Destination replyTo = jmsMessage.getJMSReplyTo();
			if (replyTo != null) {
				header.setAttribute(JmsAttributeKeys.REPLY_TO, replyTo);
			}
			header.setAttribute(JmsAttributeKeys.REDELIVERED, jmsMessage.getJMSRedelivered());
			String type = jmsMessage.getJMSType();
			if (type != null) {
				header.setAttribute(JmsAttributeKeys.TYPE, type);
			}
			Enumeration<?> jmsPropertyNames = jmsMessage.getPropertyNames();
			if (jmsPropertyNames != null) {
				while (jmsPropertyNames.hasMoreElements()) {
					String propertyName = jmsPropertyNames.nextElement().toString();
					header.setAttribute(JmsAttributeKeys.USER_DEFINED_ATTRIBUTE_PREFIX + propertyName,
							jmsMessage.getObjectProperty(propertyName));
				}
			}
		}
		catch (Throwable t) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping properties to MessageHeader", t);
			}
		}
	}

}
