/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link JmsHeaderMapper}.
 * <p>
 * This implementation copies JMS API headers (e.g. JMSReplyTo) to and from
 * Spring Integration Messages. Any user-defined properties will also be copied
 * from a JMS Message to a Spring Integration Message, and any other headers
 * on a Spring Integration Message (beyond the JMS API headers) will likewise
 * be copied to a JMS Message. Those other headers will be copied to the
 * general properties of a JMS Message whereas the JMS API headers are passed
 * to the appropriate setter methods (e.g. setJMSReplyTo).
 * <p>
 * Constants for the JMS API headers are defined in {@link JmsHeaders}.
 * Note that the JMSMessageID and JMSRedelivered flag are only copied
 * <em>from</em> a JMS Message. Those values will <em>not</em> be passed
 * along from a Spring Integration Message to an outbound JMS Message.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class DefaultJmsHeaderMapper extends JmsHeaderMapper {

	private static final List<Class<?>> SUPPORTED_PROPERTY_TYPES = Arrays.asList(new Class<?>[]{
			Boolean.class, Byte.class, Double.class, Float.class, Integer.class, Long.class, Short.class, String.class });


	private static final Log LOGGER = LogFactory.getLog(DefaultJmsHeaderMapper.class);

	private volatile String inboundPrefix = "";

	private volatile String outboundPrefix = "";

	private volatile boolean mapInboundPriority = true;

	private volatile boolean mapInboundDeliveryMode = false;

	private volatile boolean mapInboundExpiration = false;

	/**
	 * Suppress the mapping of inbound priority by using this setter with 'false'.
	 * @param mapInboundPriority 'false' to suppress mapping the inbound priority.
	 */
	public void setMapInboundPriority(boolean mapInboundPriority) {
		this.mapInboundPriority = mapInboundPriority;
	}

	/**
	 * Map the inbound {@code deliveryMode} by using this setter with 'true'.
	 * @param mapInboundDeliveryMode 'true' to map the inbound delivery mode.
	 * @since 5.1
	 */
	public void setMapInboundDeliveryMode(boolean mapInboundDeliveryMode) {
		this.mapInboundDeliveryMode = mapInboundDeliveryMode;
	}

	/**
	 * Map the inbound {@code expiration} by using this setter with 'true'.
	 * @param mapInboundExpiration 'true' to map the inbound expiration.
	 * @since 5.1
	 */
	public void setMapInboundExpiration(boolean mapInboundExpiration) {
		this.mapInboundExpiration = mapInboundExpiration;
	}

	/**
	 * Specify a prefix to be appended to the integration message header name
	 * for any JMS property that is being mapped into the MessageHeaders.
	 * The Default is an empty string (no prefix).
	 * <p>
	 * This does not affect the JMS properties covered by the specification/API,
	 * such as JMSCorrelationID, etc. The header names used for mapping such
	 * properties are all defined in our {@link JmsHeaders}.
	 *
	 * @param inboundPrefix The inbound prefix.
	 */
	public void setInboundPrefix(String inboundPrefix) {
		this.inboundPrefix = (inboundPrefix != null) ? inboundPrefix : "";
	}

	/**
	 * Specify a prefix to be appended to the JMS property name for any
	 * integration message header that is being mapped into the JMS Message.
	 * The Default is an empty string (no prefix).
	 * <p>
	 * This does not affect the JMS properties covered by the specification/API,
	 * such as JMSCorrelationID, etc. The header names used for mapping such
	 * properties are all defined in our {@link JmsHeaders}.
	 *
	 * @param outboundPrefix The outbound prefix.
	 */
	public void setOutboundPrefix(String outboundPrefix) {
		this.outboundPrefix = (outboundPrefix != null) ? outboundPrefix : "";
	}

	@Override
	public void fromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
		try {
			populateCorrelationIdPropertyFromHeaders(headers, jmsMessage);
			populateReplyToPropertyFromHeaders(headers, jmsMessage);
			populateTypePropertyFromHeaders(headers, jmsMessage);

			for (Entry<String, Object> entry : headers.entrySet()) {
				String headerName = entry.getKey();

				if (StringUtils.hasText(headerName) &&
						!headerName.startsWith(JmsHeaders.PREFIX) &&
						jmsMessage.getObjectProperty(headerName) == null) {

					Object value = entry.getValue();
					if (value != null) {
						populateArbitraryHeaderToProperty(jmsMessage, headerName, value);
					}
				}
			}
		}
		catch (Exception ex) {
			LOGGER.warn("Error occurred while mapping from MessageHeaders to JMS properties", ex);
		}
	}

	private void populateCorrelationIdPropertyFromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
		Object jmsCorrelationId = headers.get(JmsHeaders.CORRELATION_ID);
		if (jmsCorrelationId instanceof Number) {
			jmsCorrelationId = jmsCorrelationId.toString();
		}
		if (jmsCorrelationId instanceof String) {
			try {
				jmsMessage.setJMSCorrelationID((String) jmsCorrelationId);
			}
			catch (Exception ex) {
				LOGGER.info("Failed to set JMSCorrelationID, skipping", ex);
			}
		}
	}

	private void populateReplyToPropertyFromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
		Object jmsReplyTo = headers.get(JmsHeaders.REPLY_TO);
		if (jmsReplyTo instanceof Destination) {
			try {
				jmsMessage.setJMSReplyTo((Destination) jmsReplyTo);
			}
			catch (Exception ex) {
				LOGGER.info("Failed to set JMSReplyTo, skipping", ex);
			}
		}
	}

	private void populateTypePropertyFromHeaders(MessageHeaders headers, javax.jms.Message jmsMessage) {
		Object jmsType = headers.get(JmsHeaders.TYPE);
		if (jmsType instanceof String) {
			try {
				jmsMessage.setJMSType((String) jmsType);
			}
			catch (Exception ex) {
				LOGGER.info("Failed to set JMSType, skipping", ex);
			}
		}
	}

	private void populateArbitraryHeaderToProperty(javax.jms.Message jmsMessage, String headerName, Object value)
			throws JMSException {

		if (SUPPORTED_PROPERTY_TYPES.contains(value.getClass())) {
			try {
				String propertyName = fromHeaderName(headerName);
				jmsMessage.setObjectProperty(propertyName, value);
			}
			catch (Exception e) {
				if (headerName.startsWith("JMSX")
						|| headerName.equals(IntegrationMessageHeaderAccessor.PRIORITY)) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("skipping reserved header, it cannot be set by client: "
								+ headerName);
					}
				}
				else if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("failed to map Message header '" + headerName + "' to JMS property", e);
				}
			}
		}
		else if (IntegrationMessageHeaderAccessor.CORRELATION_ID.equals(headerName)) {
			String propertyName = fromHeaderName(headerName);
			jmsMessage.setObjectProperty(propertyName, value.toString());
		}
	}

	@Override
	public Map<String, Object> toHeaders(javax.jms.Message jmsMessage) {
		Map<String, Object> headers = new HashMap<>();
		try {
			mapMessageIdProperty(jmsMessage, headers);
			mapDestinationProperty(jmsMessage, headers);
			mapCorrelationIdProperty(jmsMessage, headers);
			mapReplyToProperty(jmsMessage, headers);
			mapRedeliveredProperty(jmsMessage, headers);
			mapTypeProperty(jmsMessage, headers);
			mapTimestampProperty(jmsMessage, headers);
			mapPriorityProperty(jmsMessage, headers);
			mapDeliveryModeProperty(jmsMessage, headers);
			mapExpirationProperty(jmsMessage, headers);
			Enumeration<?> jmsPropertyNames = jmsMessage.getPropertyNames();
			if (jmsPropertyNames != null) {
				while (jmsPropertyNames.hasMoreElements()) {
					String propertyName = jmsPropertyNames.nextElement().toString();
					mapArbitraryProperty(jmsMessage, headers, propertyName);
				}
			}
		}
		catch (JMSException ex) {
			LOGGER.warn("error occurred while mapping from JMS properties to MessageHeaders", ex);
		}
		return headers;
	}

	private void mapMessageIdProperty(Message jmsMessage, Map<String, Object> headers) {
		try {
			String messageId = jmsMessage.getJMSMessageID();
			if (messageId != null) {
				headers.put(JmsHeaders.MESSAGE_ID, messageId);
			}
		}
		catch (Exception ex) {
			LOGGER.info("Failed to read JMSMessageID property, skipping", ex);
		}
	}

	private void mapDestinationProperty(Message jmsMessage, Map<String, Object> headers) {
		try {
			Destination destination = jmsMessage.getJMSDestination();
			if (destination != null) {
				headers.put(JmsHeaders.DESTINATION, destination);
			}
		}
		catch (Exception ex) {
			LOGGER.info("Failed to read JMSDestination property, skipping", ex);
		}
	}

	private void mapCorrelationIdProperty(Message jmsMessage, Map<String, Object> headers) {
		try {
			String correlationId = jmsMessage.getJMSCorrelationID();
			if (correlationId != null) {
				headers.put(JmsHeaders.CORRELATION_ID, correlationId);
			}
		}
		catch (Exception ex) {
			LOGGER.info("Failed to read JMSCorrelationID property, skipping", ex);
		}
	}

	private void mapReplyToProperty(Message jmsMessage, Map<String, Object> headers) {
		try {
			Destination replyTo = jmsMessage.getJMSReplyTo();
			if (replyTo != null) {
				headers.put(JmsHeaders.REPLY_TO, replyTo);
			}
		}
		catch (Exception ex) {
			LOGGER.info("failed to read JMSReplyTo property, skipping", ex);
		}
	}

	private void mapRedeliveredProperty(Message jmsMessage, Map<String, Object> headers) {
		try {
			headers.put(JmsHeaders.REDELIVERED, jmsMessage.getJMSRedelivered());
		}
		catch (Exception ex) {
			LOGGER.info("failed to read JMSRedelivered property, skipping", ex);
		}
	}

	private void mapTypeProperty(Message jmsMessage, Map<String, Object> headers) {
		try {
			String type = jmsMessage.getJMSType();
			if (type != null) {
				headers.put(JmsHeaders.TYPE, type);
			}
		}
		catch (Exception ex) {
			LOGGER.info("Failed to read JMSType property, skipping", ex);
		}
	}

	private void mapTimestampProperty(Message jmsMessage, Map<String, Object> headers) {
		try {
			headers.put(JmsHeaders.TIMESTAMP, jmsMessage.getJMSTimestamp());
		}
		catch (Exception ex) {
			LOGGER.info("failed to read JMSTimestamp property, skipping", ex);
		}
	}

	private void mapPriorityProperty(Message jmsMessage, Map<String, Object> headers) {
		if (this.mapInboundPriority) {
			try {
				headers.put(IntegrationMessageHeaderAccessor.PRIORITY, jmsMessage.getJMSPriority());
			}
			catch (Exception ex) {
				LOGGER.info("Failed to read JMSPriority property, skipping", ex);
			}
		}
	}

	private void mapDeliveryModeProperty(Message jmsMessage, Map<String, Object> headers) {
		if (this.mapInboundDeliveryMode) {
			try {
				headers.put(JmsHeaders.DELIVERY_MODE, jmsMessage.getJMSDeliveryMode());
			}
			catch (Exception ex) {
				LOGGER.info("Failed to read JMSDeliveryMode property, skipping", ex);
			}
		}
	}

	private void mapExpirationProperty(Message jmsMessage, Map<String, Object> headers) {
		if (this.mapInboundExpiration) {
			try {
				headers.put(JmsHeaders.EXPIRATION, jmsMessage.getJMSExpiration());
			}
			catch (Exception ex) {
				LOGGER.info("Failed to read JMSExpiration property, skipping", ex);
			}
		}
	}

	private void mapArbitraryProperty(Message jmsMessage, Map<String, Object> headers, String propertyName) {
		try {
			String headerName = toHeaderName(propertyName);
			headers.put(headerName, jmsMessage.getObjectProperty(propertyName));
		}
		catch (Exception ex) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn("Error occurred while mapping JMS property '" + propertyName + "' to Message header", ex);
			}
		}
	}

	/**
	 * Adds the outbound prefix if necessary.
	 * Converts {@link MessageHeaders#CONTENT_TYPE} to content_type for JMS compliance.
	 */
	private String fromHeaderName(String headerName) {
		String propertyName = headerName;
		if (StringUtils.hasText(this.outboundPrefix) && !propertyName.startsWith(this.outboundPrefix)) {
			propertyName = this.outboundPrefix + headerName;
		}
		else if (MessageHeaders.CONTENT_TYPE.equals(headerName)) {
			propertyName = CONTENT_TYPE_PROPERTY;
		}
		return propertyName;
	}

	/**
	 * Adds the inbound prefix if necessary.
	 * Converts content_type to {@link MessageHeaders#CONTENT_TYPE}.
	 */
	private String toHeaderName(String propertyName) {
		String headerName = propertyName;
		if (StringUtils.hasText(this.inboundPrefix) && !headerName.startsWith(this.inboundPrefix)) {
			headerName = this.inboundPrefix + propertyName;
		}
		else if (CONTENT_TYPE_PROPERTY.equals(propertyName)) {
			headerName = MessageHeaders.CONTENT_TYPE;
		}
		return headerName;
	}

}

