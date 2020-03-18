/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.amqp.support;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.utils.JavaUtils;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link AmqpHeaderMapper}.
 * <p>
 * By default this implementation will only copy AMQP properties (e.g. contentType) to and from
 * Spring Integration MessageHeaders. Any user-defined headers within the AMQP
 * MessageProperties will NOT be copied to or from an AMQP Message unless
 * explicitly identified via 'requestHeaderNames' and/or 'replyHeaderNames'
 * (see {@link AbstractHeaderMapper#setRequestHeaderNames(String[])}
 * and {@link AbstractHeaderMapper#setReplyHeaderNames(String[])}}
 * as well as 'mapped-request-headers' and 'mapped-reply-headers' attributes of the AMQP adapters).
 * If you need to copy all user-defined headers simply use wild-card character '*'.
 * <p>
 * Constants for the AMQP header keys are defined in {@link AmqpHeaders}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Stephane Nicoll
 * @author Steve Singer
 *
 * @since 2.1
 */
public class DefaultAmqpHeaderMapper extends AbstractHeaderMapper<MessageProperties> implements AmqpHeaderMapper {

	private static final List<String> STANDARD_HEADER_NAMES = new ArrayList<>();

	static {
		STANDARD_HEADER_NAMES.add(AmqpHeaders.APP_ID);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.CLUSTER_ID);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.CONTENT_ENCODING);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.CONTENT_LENGTH);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.CONTENT_TYPE);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.CORRELATION_ID);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.DELAY);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.DELIVERY_MODE);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.DELIVERY_TAG);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.EXPIRATION);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.MESSAGE_COUNT);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.MESSAGE_ID);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.RECEIVED_DELAY);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.RECEIVED_DELIVERY_MODE);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.RECEIVED_EXCHANGE);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.RECEIVED_ROUTING_KEY);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.REDELIVERED);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.REPLY_TO);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.TIMESTAMP);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.TYPE);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.USER_ID);
		STANDARD_HEADER_NAMES.add(JsonHeaders.TYPE_ID);
		STANDARD_HEADER_NAMES.add(JsonHeaders.CONTENT_TYPE_ID);
		STANDARD_HEADER_NAMES.add(JsonHeaders.KEY_TYPE_ID);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.SPRING_REPLY_CORRELATION);
		STANDARD_HEADER_NAMES.add(AmqpHeaders.SPRING_REPLY_TO_STACK);
	}

	protected DefaultAmqpHeaderMapper(String[] requestHeaderNames, String[] replyHeaderNames) {
		super(AmqpHeaders.PREFIX, STANDARD_HEADER_NAMES, STANDARD_HEADER_NAMES);
		if (requestHeaderNames != null) {
			setRequestHeaderNames(requestHeaderNames);
		}
		if (replyHeaderNames != null) {
			setReplyHeaderNames(replyHeaderNames);
		}
	}

	/**
	 * Extract "standard" headers from an AMQP MessageProperties instance.
	 */
	@Override
	protected Map<String, Object> extractStandardHeaders(MessageProperties amqpMessageProperties) {
		Map<String, Object> headers = new HashMap<>();
		try {
			JavaUtils.INSTANCE
					.acceptIfNotNull(AmqpHeaders.APP_ID, amqpMessageProperties.getAppId(), headers::put)
					.acceptIfNotNull(AmqpHeaders.CLUSTER_ID, amqpMessageProperties.getClusterId(), headers::put)
					.acceptIfNotNull(AmqpHeaders.CONTENT_ENCODING, amqpMessageProperties.getContentEncoding(),
							headers::put);
			long contentLength = amqpMessageProperties.getContentLength();
			JavaUtils.INSTANCE
					.acceptIfCondition(contentLength > 0, AmqpHeaders.CONTENT_LENGTH, contentLength, headers::put)
					.acceptIfHasText(AmqpHeaders.CONTENT_TYPE, amqpMessageProperties.getContentType(), headers::put)
					.acceptIfHasText(AmqpHeaders.CORRELATION_ID, amqpMessageProperties.getCorrelationId(), headers::put)
					.acceptIfNotNull(AmqpHeaders.RECEIVED_DELIVERY_MODE,
							amqpMessageProperties.getReceivedDeliveryMode(), headers::put);
			long deliveryTag = amqpMessageProperties.getDeliveryTag();
			JavaUtils.INSTANCE
					.acceptIfCondition(deliveryTag > 0, AmqpHeaders.DELIVERY_TAG, deliveryTag, headers::put)
					.acceptIfHasText(AmqpHeaders.EXPIRATION, amqpMessageProperties.getExpiration(), headers::put);
			Integer messageCount = amqpMessageProperties.getMessageCount();
			JavaUtils.INSTANCE
					.acceptIfCondition(messageCount != null && messageCount > 0, AmqpHeaders.MESSAGE_COUNT,
							messageCount, headers::put)
					.acceptIfHasText(AmqpHeaders.MESSAGE_ID, amqpMessageProperties.getMessageId(), headers::put);
			Integer priority = amqpMessageProperties.getPriority();
			JavaUtils.INSTANCE
					.acceptIfCondition(priority != null && priority > 0, IntegrationMessageHeaderAccessor.PRIORITY,
							priority, headers::put)
					.acceptIfNotNull(AmqpHeaders.RECEIVED_DELAY, amqpMessageProperties.getReceivedDelay(), headers::put)
					.acceptIfNotNull(AmqpHeaders.RECEIVED_EXCHANGE, amqpMessageProperties.getReceivedExchange(),
							headers::put)
					.acceptIfHasText(AmqpHeaders.RECEIVED_ROUTING_KEY, amqpMessageProperties.getReceivedRoutingKey(),
							headers::put)
					.acceptIfNotNull(AmqpHeaders.REDELIVERED, amqpMessageProperties.isRedelivered(), headers::put)
					.acceptIfNotNull(AmqpHeaders.REPLY_TO, amqpMessageProperties.getReplyTo(), headers::put)
					.acceptIfNotNull(AmqpHeaders.TIMESTAMP, amqpMessageProperties.getTimestamp(), headers::put)
					.acceptIfHasText(AmqpHeaders.TYPE, amqpMessageProperties.getType(), headers::put)
					.acceptIfHasText(AmqpHeaders.RECEIVED_USER_ID,
							amqpMessageProperties.getReceivedUserId(), headers::put);

			for (String jsonHeader : JsonHeaders.HEADERS) {
				Object value = amqpMessageProperties.getHeaders().get(jsonHeader.replaceFirst(JsonHeaders.PREFIX, ""));
				if (value instanceof String && StringUtils.hasText((String) value)) {
					headers.put(jsonHeader, value);
				}
			}
		}
		catch (Exception e) {
			this.logger.warn("error occurred while mapping from AMQP properties to MessageHeaders", e);
		}
		return headers;
	}

	/**
	 * Extract user-defined headers from an AMQP MessageProperties instance.
	 */
	@Override
	protected Map<String, Object> extractUserDefinedHeaders(MessageProperties amqpMessageProperties) {
		return amqpMessageProperties.getHeaders();
	}

	/**
	 * Maps headers from a Spring Integration MessageHeaders instance to the MessageProperties
	 * of an AMQP Message.
	 */
	@Override
	protected void populateStandardHeaders(Map<String, Object> headers, MessageProperties amqpMessageProperties) {
		populateStandardHeaders(null, headers, amqpMessageProperties);
	}

	/**
	 * Maps headers from a Spring Integration MessageHeaders instance to the MessageProperties
	 * of an AMQP Message.
	 */
	@Override
	protected void populateStandardHeaders(@Nullable Map<String, Object> allHeaders, Map<String, Object> headers,
			MessageProperties amqpMessageProperties) {

		JavaUtils.INSTANCE
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.APP_ID, String.class),
						amqpMessageProperties::setAppId)
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.CLUSTER_ID, String.class),
						amqpMessageProperties::setClusterId)
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.CONTENT_ENCODING, String.class),
						amqpMessageProperties::setContentEncoding)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.CONTENT_LENGTH, Long.class),
						amqpMessageProperties::setContentLength)
				.acceptIfHasText(this.extractContentTypeAsString(headers),
						amqpMessageProperties::setContentType)
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.CORRELATION_ID, String.class),
						amqpMessageProperties::setCorrelationId)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.DELAY, Integer.class),
						amqpMessageProperties::setDelay)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.class),
						amqpMessageProperties::setDeliveryMode)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.DELIVERY_TAG, Long.class),
						amqpMessageProperties::setDeliveryTag)
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.EXPIRATION, String.class),
						amqpMessageProperties::setExpiration)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.MESSAGE_COUNT, Integer.class),
						amqpMessageProperties::setMessageCount);
		String messageId = getHeaderIfAvailable(headers, AmqpHeaders.MESSAGE_ID, String.class);
		if (StringUtils.hasText(messageId)) {
			amqpMessageProperties.setMessageId(messageId);
		}
		else if (allHeaders != null) {
			UUID id = getHeaderIfAvailable(allHeaders, MessageHeaders.ID, UUID.class);
			if (id != null) {
				amqpMessageProperties.setMessageId(id.toString());
			}
		}
		JavaUtils.INSTANCE
				.acceptIfNotNull(getHeaderIfAvailable(headers, IntegrationMessageHeaderAccessor.PRIORITY,
						Integer.class),
						amqpMessageProperties::setPriority)
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.RECEIVED_EXCHANGE, String.class),
						amqpMessageProperties::setReceivedExchange)
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.RECEIVED_ROUTING_KEY, String.class),
						amqpMessageProperties::setReceivedRoutingKey)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.REDELIVERED, Boolean.class),
						amqpMessageProperties::setRedelivered)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.REPLY_TO, String.class),
						amqpMessageProperties::setReplyTo);
		Date timestamp = getHeaderIfAvailable(headers, AmqpHeaders.TIMESTAMP, Date.class);
		if (timestamp != null) {
			amqpMessageProperties.setTimestamp(timestamp);
		}
		else if (allHeaders != null) {
			Long ts = getHeaderIfAvailable(allHeaders, MessageHeaders.TIMESTAMP, Long.class);
			if (ts != null) {
				amqpMessageProperties.setTimestamp(new Date(ts));
			}
		}
		JavaUtils.INSTANCE
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.TYPE, String.class),
						amqpMessageProperties::setType)
				.acceptIfNotNull(getHeaderIfAvailable(headers, AmqpHeaders.USER_ID, String.class),
						amqpMessageProperties::setUserId);

		mapJsonHeaders(headers, amqpMessageProperties);

		JavaUtils.INSTANCE
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.SPRING_REPLY_CORRELATION, String.class),
						replyCorrelation -> amqpMessageProperties
								.setHeader("spring_reply_correlation", replyCorrelation))
				.acceptIfHasText(getHeaderIfAvailable(headers, AmqpHeaders.SPRING_REPLY_TO_STACK, String.class),
						replyToStack -> amqpMessageProperties.setHeader("spring_reply_to", replyToStack));
	}

	private void mapJsonHeaders(Map<String, Object> headers, MessageProperties amqpMessageProperties) {
		/*
		 * If the MessageProperties already contains JsonHeaders, don't overwrite them here because they were
		 * set up by a message converter.
		 */
		if (!amqpMessageProperties.getHeaders().containsKey(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, ""))) {
			Map<String, String> jsonHeaders = new HashMap<>();

			for (String jsonHeader : JsonHeaders.HEADERS) {
				if (!JsonHeaders.RESOLVABLE_TYPE.equals(jsonHeader)) {
					Object value = getHeaderIfAvailable(headers, jsonHeader, Object.class);
					if (value != null) {
						headers.remove(jsonHeader);
						if (value instanceof Class<?>) {
							value = ((Class<?>) value).getName();
						}
						jsonHeaders.put(jsonHeader.replaceFirst(JsonHeaders.PREFIX, ""), value.toString());
					}
				}
			}
			amqpMessageProperties.getHeaders().putAll(jsonHeaders);
		}
	}

	@Override
	protected void populateUserDefinedHeader(String headerName, Object headerValue,
			MessageProperties amqpMessageProperties) {
		// do not overwrite an existing header with the same key
		// TODO: do we need to expose a boolean 'overwrite' flag?
		if (!amqpMessageProperties.getHeaders().containsKey(headerName) &&
				!AmqpHeaders.CONTENT_TYPE.equals(headerName) &&
				!headerName.startsWith(JsonHeaders.PREFIX)) {
			amqpMessageProperties.setHeader(headerName, headerValue);
		}
	}

	/**
	 * Will extract Content-Type from MessageHeaders and convert it to String if possible
	 * Required since Content-Type can be represented as org.springframework.util.MimeType.
	 *
	 */
	private String extractContentTypeAsString(Map<String, Object> headers) {
		String contentTypeStringValue = null;

		Object contentType = getHeaderIfAvailable(headers, AmqpHeaders.CONTENT_TYPE, Object.class);

		if (contentType != null) {
			String contentTypeClassName = contentType.getClass().getName();

			if (contentType instanceof MimeType) {
				contentTypeStringValue = contentType.toString();
			}
			else if (contentType instanceof String) {
				contentTypeStringValue = (String) contentType;
			}
			else {
				if (logger.isWarnEnabled()) {
					logger.warn("skipping header '" + AmqpHeaders.CONTENT_TYPE +
							"' since it is not of expected type [" + contentTypeClassName + "]");
				}
			}
		}
		return contentTypeStringValue;
	}

	@Override
	public Map<String, Object> toHeadersFromRequest(MessageProperties source) {
		Map<String, Object> headersFromRequest = super.toHeadersFromRequest(source);
		addConsumerMetadata(source, headersFromRequest);
		return headersFromRequest;
	}

	private void addConsumerMetadata(MessageProperties messageProperties, Map<String, Object> headers) {
		String consumerTag = messageProperties.getConsumerTag();
		if (consumerTag != null) {
			headers.put(AmqpHeaders.CONSUMER_TAG, consumerTag);
		}
		String consumerQueue = messageProperties.getConsumerQueue();
		if (consumerQueue != null) {
			headers.put(AmqpHeaders.CONSUMER_QUEUE, consumerQueue);
		}
	}

	/**
	 * Construct a default inbound header mapper.
	 * @return the mapper.
	 * @see #inboundRequestHeaders()
	 * @see #inboundReplyHeaders()
	 * @since 4.3
	 */
	public static DefaultAmqpHeaderMapper inboundMapper() {
		return new DefaultAmqpHeaderMapper(inboundRequestHeaders(), inboundReplyHeaders());
	}

	/**
	 * Construct a default outbound header mapper.
	 * @return the mapper.
	 * @see #outboundRequestHeaders()
	 * @see #outboundReplyHeaders()
	 * @since 4.3
	 */
	public static DefaultAmqpHeaderMapper outboundMapper() {
		return new DefaultAmqpHeaderMapper(outboundRequestHeaders(), outboundReplyHeaders());
	}

	/**
	 * @return the default request headers for an inbound mapper.
	 * @since 4.3
	 */
	public static String[] inboundRequestHeaders() {
		return new String[] { "*" };
	}

	/**
	 * @return the default reply headers for an inbound mapper.
	 * @since 4.3
	 */
	public static String[] inboundReplyHeaders() {
		return safeOutboundHeaders();
	}

	/**
	 * @return the default request headers for an outbound mapper.
	 * @since 4.3
	 */
	public static String[] outboundRequestHeaders() {
		return safeOutboundHeaders();
	}

	/**
	 * @return the default reply headers for an outbound mapper.
	 * @since 4.3
	 */
	public static String[] outboundReplyHeaders() {
		return new String[] { "*" };
	}

	private static String[] safeOutboundHeaders() {
		return new String[] { "!x-*", "*" };
	}

}
