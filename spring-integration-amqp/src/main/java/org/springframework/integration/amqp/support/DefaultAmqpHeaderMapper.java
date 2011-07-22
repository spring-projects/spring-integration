/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.amqp.support;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link AmqpHeaderMapper}.
 * <p/>
 * This implementation copies AMQP properties (e.g. contentType) to and from
 * Spring Integration MessageHeaders. Any user-defined headers within the AMQP
 * MessageProperties will also be copied from an AMQP Message to a Spring Integration
 * Message, and any other headers on a Spring Integration Message (beyond the standard
 * AMQP properties) will likewise be copied to an AMQP Message.
 * <p/>
 * Constants for the AMQP header keys are defined in {@link AmqpHeaders}.
 * 
 * @author Mark Fisher
 */
public class DefaultAmqpHeaderMapper implements AmqpHeaderMapper {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile String inboundPrefix = "";

	private volatile String outboundPrefix = "";


	/**
	 * Specify a prefix to be appended to the integration message header name for
	 * any user-defined AMQP header that is being mapped into the MessageHeaders.
	 * The Default is an empty string (no prefix).
	 * <p/>
	 * This does not affect the standard AMQP properties, such as contentType, etc.
	 * The header names used for mapping such properties are all defined in the
	 * {@link AmqpHeaders} class as constants.   
	 */
	public void setInboundPrefix(String inboundPrefix) {
		this.inboundPrefix = (inboundPrefix != null) ? inboundPrefix : "";
	}

	/**
	 * Specify a prefix to be appended to the AMQP header name for any
	 * integration message header that is being mapped into the AMQP Message.
	 * The Default is an empty string (no prefix).
	 * <p/>
	 * This does not affect the standard AMQP properties, such as contentType, etc.
	 * The header names used for mapping such properties are all defined in
	 * the {@link AmqpHeaders} class as constants.   
	 */
	public void setOutboundPrefix(String outboundPrefix) {
		this.outboundPrefix = (outboundPrefix != null) ? outboundPrefix : "";
	}

	/**
	 * Maps headers from a Spring Integration MessageHeaders instance to the MessageProperties
	 * of an AMQP Message.
	 */
	public void fromHeaders(MessageHeaders headers, MessageProperties amqpMessageProperties) {
		try {
			String appId = getHeaderIfAvailable(headers, AmqpHeaders.APP_ID, String.class);
			if (StringUtils.hasText(appId)) {
				amqpMessageProperties.setAppId(appId);
			}				
			String clusterId = getHeaderIfAvailable(headers, AmqpHeaders.CLUSTER_ID, String.class);
			if (StringUtils.hasText(clusterId)) {
				amqpMessageProperties.setClusterId(clusterId);
			}
			String contentEncoding = getHeaderIfAvailable(headers, AmqpHeaders.CONTENT_ENCODING, String.class);
			if (StringUtils.hasText(contentEncoding)) {
				amqpMessageProperties.setContentEncoding(contentEncoding);
			}
			Long contentLength = getHeaderIfAvailable(headers, AmqpHeaders.CONTENT_LENGTH, Long.class);
			if (contentLength != null) {
				amqpMessageProperties.setContentLength(contentLength);
			}			
			String contentType = getHeaderIfAvailable(headers, AmqpHeaders.CONTENT_TYPE, String.class);
			if (StringUtils.hasText(contentType)) {
				amqpMessageProperties.setContentType(contentType);
			}
			Object correlationId = headers.get(AmqpHeaders.CORRELATION_ID);
			if (correlationId instanceof byte[]) {
				amqpMessageProperties.setCorrelationId((byte[]) correlationId);
			}
			MessageDeliveryMode deliveryMode = getHeaderIfAvailable(headers, AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.class);
			if (deliveryMode != null) {
				amqpMessageProperties.setDeliveryMode(deliveryMode);
			}
			Long deliveryTag = getHeaderIfAvailable(headers, AmqpHeaders.DELIVERY_TAG, Long.class);
			if (deliveryTag != null) {
				amqpMessageProperties.setDeliveryTag(deliveryTag);
			}
			String expiration = getHeaderIfAvailable(headers, AmqpHeaders.EXPIRATION, String.class);
			if (StringUtils.hasText(expiration)) {
				amqpMessageProperties.setExpiration(expiration);
			}
			Integer messageCount = getHeaderIfAvailable(headers, AmqpHeaders.MESSAGE_COUNT, Integer.class);
			if (messageCount != null) {
				amqpMessageProperties.setMessageCount(messageCount);
			}
			String messageId = getHeaderIfAvailable(headers, AmqpHeaders.MESSAGE_ID, String.class);
			if (StringUtils.hasText(messageId)) {
				amqpMessageProperties.setMessageId(messageId);
			}
			Integer priority = headers.getPriority();
			if (priority != null) {
				amqpMessageProperties.setPriority(priority);
			}
			String receivedExchange = getHeaderIfAvailable(headers, AmqpHeaders.RECEIVED_EXCHANGE, String.class);
			if (StringUtils.hasText(receivedExchange)) {
				amqpMessageProperties.setReceivedExchange(receivedExchange);
			}
			String receivedRoutingKey = getHeaderIfAvailable(headers, AmqpHeaders.RECEIVED_ROUTING_KEY, String.class);
			if (StringUtils.hasText(receivedRoutingKey)) {
				amqpMessageProperties.setReceivedRoutingKey(receivedRoutingKey);
			}
			Boolean redelivered = getHeaderIfAvailable(headers, AmqpHeaders.REDELIVERED, Boolean.class);
			if (redelivered != null) {
				amqpMessageProperties.setRedelivered(redelivered);
			}
			Address replyTo = getHeaderIfAvailable(headers, AmqpHeaders.REPLY_TO, Address.class);
			if (replyTo != null) {
				amqpMessageProperties.setReplyTo(replyTo);
			}
			Date timestamp = getHeaderIfAvailable(headers, AmqpHeaders.TIMESTAMP, Date.class);
			if (timestamp != null) {
				amqpMessageProperties.setTimestamp(timestamp);
			}
			String type = getHeaderIfAvailable(headers, AmqpHeaders.TYPE, String.class);
			if (type != null) {
				amqpMessageProperties.setType(type);
			}
			String userId = getHeaderIfAvailable(headers, AmqpHeaders.USER_ID, String.class);
			if (StringUtils.hasText(userId)) {
				amqpMessageProperties.setUserId(userId);
			}
			// now map to the user-defined headers, if any, within the AMQP MessageProperties
			Set<String> headerNames = headers.keySet();
			for (String headerName : headerNames) {
				if (StringUtils.hasText(headerName) && !headerName.startsWith(AmqpHeaders.PREFIX)) {
					Object value = headers.get(headerName);
					if (value != null) {
						try {
							String key = this.fromHeaderName(headerName);
							amqpMessageProperties.setHeader(key, value);
						}
						catch (Exception e) {
							if (logger.isWarnEnabled()) {
								logger.warn("failed to map Message header '" + headerName + "' to AMQP header", e);
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping from MessageHeaders to AMQP properties", e);
			}
		}
	}

	/**
	 * Maps headers from an AMQP MessageProperties instance to the MessageHeaders of a
	 * Spring Integration Message.
	 */
	public Map<String, Object> toHeaders(MessageProperties amqpMessageProperties) {
		Map<String, Object> headers = new HashMap<String, Object>();
		try {
			String appId = amqpMessageProperties.getAppId();
			if (StringUtils.hasText(appId)) {
				headers.put(AmqpHeaders.APP_ID, appId);
			}
			String clusterId = amqpMessageProperties.getClusterId();
			if (StringUtils.hasText(clusterId)) {
				headers.put(AmqpHeaders.CLUSTER_ID, clusterId);
			}
			String contentEncoding = amqpMessageProperties.getContentEncoding();
			if (StringUtils.hasText(contentEncoding)) {
				headers.put(AmqpHeaders.CONTENT_ENCODING, contentEncoding);
			}
			long contentLength = amqpMessageProperties.getContentLength();
			if (contentLength > 0) {
				headers.put(AmqpHeaders.CONTENT_LENGTH, contentLength);
			}
			String contentType = amqpMessageProperties.getContentType();
			if (StringUtils.hasText(contentType)) {
				headers.put(AmqpHeaders.CONTENT_TYPE, contentType);
			}
			byte[] correlationId = amqpMessageProperties.getCorrelationId();
			if (correlationId != null && correlationId.length > 0) {
				headers.put(AmqpHeaders.CORRELATION_ID, correlationId);
			}
			MessageDeliveryMode deliveryMode = amqpMessageProperties.getDeliveryMode();
			if (deliveryMode != null) {
				headers.put(AmqpHeaders.DELIVERY_MODE, deliveryMode);
			}
			long deliveryTag = amqpMessageProperties.getDeliveryTag();
			if (deliveryTag > 0) {
				headers.put(AmqpHeaders.DELIVERY_TAG, deliveryTag);
			}
			String expiration = amqpMessageProperties.getExpiration();
			if (StringUtils.hasText(expiration)) {
				headers.put(AmqpHeaders.EXPIRATION, expiration);
			}
			Integer messageCount = amqpMessageProperties.getMessageCount();
			if (messageCount != null && messageCount > 0) {
				headers.put(AmqpHeaders.MESSAGE_COUNT, messageCount);
			}
			String messageId = amqpMessageProperties.getMessageId();
			if (StringUtils.hasText(messageId)) {
				headers.put(AmqpHeaders.MESSAGE_ID, messageId);
			}
			Integer priority = amqpMessageProperties.getPriority();
			if (priority != null && priority > 0) {
				headers.put(MessageHeaders.PRIORITY, priority);
			}
			String receivedExchange = amqpMessageProperties.getReceivedExchange();
			if (StringUtils.hasText(receivedExchange)) {
				headers.put(AmqpHeaders.RECEIVED_EXCHANGE, receivedExchange);
			}
			String receivedRoutingKey = amqpMessageProperties.getReceivedRoutingKey();
			if (StringUtils.hasText(receivedRoutingKey)) {
				headers.put(AmqpHeaders.RECEIVED_ROUTING_KEY, receivedRoutingKey);
			}
			Boolean redelivered = amqpMessageProperties.isRedelivered();
			if (redelivered != null) {
				headers.put(AmqpHeaders.REDELIVERED, redelivered);
			}
			Address replyTo = amqpMessageProperties.getReplyTo();
			if (replyTo != null) {
				headers.put(AmqpHeaders.REPLY_TO, replyTo);
			}
			Date timestamp = amqpMessageProperties.getTimestamp();
			if (timestamp != null) {
				headers.put(AmqpHeaders.TIMESTAMP, timestamp);
			}
			String type = amqpMessageProperties.getType();
			if (StringUtils.hasText(type)) {
				headers.put(AmqpHeaders.TYPE, type);
			}
			String userId = amqpMessageProperties.getUserId();
			if (StringUtils.hasText(userId)) {
				headers.put(AmqpHeaders.USER_ID, userId);
			}
			Map<String, Object> amqpHeaders = amqpMessageProperties.getHeaders();
			if (!CollectionUtils.isEmpty(amqpHeaders)) {
				for (Map.Entry<String, Object> entry : amqpHeaders.entrySet()) {
					try {
						String headerName = this.toHeaderName(entry.getKey());
						headers.put(headerName, entry.getValue());
					}
					catch (Exception e) {
						if (logger.isWarnEnabled()) {
							logger.warn("error occurred while mapping AMQP header '"
									+ entry.getKey() + "' to Message header", e);
						}
					}
				}
			}
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping from AMQP properties to MessageHeaders", e);
			}
		}
		return headers;
	}

	private <T> T getHeaderIfAvailable(MessageHeaders headers, String name, Class<T> type) {
		try {
			return headers.get(name, type);
		}
		catch (IllegalArgumentException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("skipping header '" + name + "' since it is not of expected type [" + type + "]", e);
			}
			return null;
		}
	}

	/**
	 * Adds the outbound prefix if necessary.
	 */
	private String fromHeaderName(String headerName) {
		String propertyName = headerName;
		if (StringUtils.hasText(this.outboundPrefix) && !propertyName.startsWith(this.outboundPrefix)) {
			propertyName = this.outboundPrefix + headerName;
		}
		return propertyName;
	}

	/**
	 * Adds the inbound prefix if necessary.
	 */
	private String toHeaderName(String propertyName) {
		String headerName = propertyName;
		if (StringUtils.hasText(this.inboundPrefix) && !headerName.startsWith(this.inboundPrefix)) {
			headerName = this.inboundPrefix + propertyName;
		}
		return headerName;
	}

}
