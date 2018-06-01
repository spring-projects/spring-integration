/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @since 2.1
 */
public class DefaultAmqpHeaderMapperTests {

	@Test
	public void fromHeadersFallbackIdTimestamp() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();
		org.springframework.messaging.Message<?> message = new GenericMessage<>("");
		MessageProperties messageProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(message.getHeaders(), messageProperties);
		assertThat(message.getHeaders().getId().toString()).isEqualTo(messageProperties.getMessageId());
		assertThat(message.getHeaders().getTimestamp()).isEqualTo(messageProperties.getTimestamp().getTime());
	}

	@Test
	public void fromHeaders() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(AmqpHeaders.APP_ID, "test.appId");
		headerMap.put(AmqpHeaders.CLUSTER_ID, "test.clusterId");
		headerMap.put(AmqpHeaders.CONTENT_ENCODING, "test.contentEncoding");
		headerMap.put(AmqpHeaders.CONTENT_LENGTH, 99L);
		headerMap.put(AmqpHeaders.CONTENT_TYPE, "test.contentType");
		String testCorrelationId = "foo";
		headerMap.put(AmqpHeaders.CORRELATION_ID, testCorrelationId);
		headerMap.put(AmqpHeaders.DELAY, 1234);
		headerMap.put(AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.NON_PERSISTENT);
		headerMap.put(AmqpHeaders.DELIVERY_TAG, 1234L);
		headerMap.put(AmqpHeaders.EXPIRATION, "test.expiration");
		headerMap.put(AmqpHeaders.MESSAGE_COUNT, 42);
		headerMap.put(AmqpHeaders.MESSAGE_ID, "test.messageId");
		headerMap.put(AmqpHeaders.RECEIVED_EXCHANGE, "test.receivedExchange");
		headerMap.put(AmqpHeaders.RECEIVED_ROUTING_KEY, "test.receivedRoutingKey");
		headerMap.put(AmqpHeaders.REPLY_TO, "test.replyTo");
		Date testTimestamp = new Date();
		headerMap.put(AmqpHeaders.TIMESTAMP, testTimestamp);
		headerMap.put(AmqpHeaders.TYPE, "test.type");
		headerMap.put(AmqpHeaders.USER_ID, "test.userId");
		headerMap.put(AmqpHeaders.SPRING_REPLY_CORRELATION, "test.correlation");
		headerMap.put(AmqpHeaders.SPRING_REPLY_TO_STACK, "test.replyTo2");

		headerMap.put(MessageHeaders.ERROR_CHANNEL, mock(MessageChannel.class));
		headerMap.put(MessageHeaders.REPLY_CHANNEL, mock(MessageChannel.class));

		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);
		Set<String> headerKeys = amqpProperties.getHeaders().keySet();
		for (String headerKey : headerKeys) {
			if (headerKey.startsWith(AmqpHeaders.PREFIX) || headerKey.equals("contentType")) {
				fail("Unexpected header in properties.headers: " + headerKey);
			}
		}
		assertEquals("test.appId", amqpProperties.getAppId());
		assertEquals("test.clusterId", amqpProperties.getClusterId());
		assertEquals("test.contentEncoding", amqpProperties.getContentEncoding());
		assertEquals(99L, amqpProperties.getContentLength());
		assertEquals("test.contentType", amqpProperties.getContentType());
		assertEquals(testCorrelationId, amqpProperties.getCorrelationId());
		assertEquals(Integer.valueOf(1234), amqpProperties.getDelay());
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, amqpProperties.getDeliveryMode());
		assertEquals(1234L, amqpProperties.getDeliveryTag());
		assertEquals("test.expiration", amqpProperties.getExpiration());
		assertEquals(new Integer(42), amqpProperties.getMessageCount());
		assertEquals("test.messageId", amqpProperties.getMessageId());
		assertEquals("test.receivedExchange", amqpProperties.getReceivedExchange());
		assertEquals("test.receivedRoutingKey", amqpProperties.getReceivedRoutingKey());
		assertEquals("test.replyTo", amqpProperties.getReplyTo());
		assertEquals(testTimestamp, amqpProperties.getTimestamp());
		assertEquals("test.type", amqpProperties.getType());
		assertEquals("test.userId", amqpProperties.getUserId());

		assertNull(amqpProperties.getHeaders().get(MessageHeaders.ERROR_CHANNEL));
		assertNull(amqpProperties.getHeaders().get(MessageHeaders.REPLY_CHANNEL));
	}

	@Test
	public void fromHeadersWithContentTypeAsMediaType() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();

		MediaType contentType = MediaType.parseMediaType("text/html");
		headerMap.put(AmqpHeaders.CONTENT_TYPE, contentType);

		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);

		assertEquals("text/html", amqpProperties.getContentType());

		headerMap.put(AmqpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
		integrationHeaders = new MessageHeaders(headerMap);
		amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);

		assertEquals(MimeTypeUtils.APPLICATION_JSON_VALUE, amqpProperties.getContentType());
	}

	@Test
	public void fromHeadersWithContentTypeAsMimeType() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();

		MimeType contentType = MimeType.valueOf("text/html");
		headerMap.put(AmqpHeaders.CONTENT_TYPE, contentType);

		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);

		assertEquals("text/html", amqpProperties.getContentType());
	}


	@Test
	public void toHeaders() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		String testCorrelationId = "foo";
		amqpProperties.setCorrelationId(testCorrelationId);
		amqpProperties.setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
		amqpProperties.setDeliveryTag(1234L);
		amqpProperties.setExpiration("test.expiration");
		amqpProperties.setMessageCount(42);
		amqpProperties.setMessageId("test.messageId");
		amqpProperties.setPriority(22);
		amqpProperties.setReceivedDelay(4567);
		amqpProperties.setReceivedExchange("test.receivedExchange");
		amqpProperties.setReceivedRoutingKey("test.receivedRoutingKey");
		amqpProperties.setRedelivered(true);
		amqpProperties.setReplyTo("test.replyTo");
		Date testTimestamp = new Date();
		amqpProperties.setTimestamp(testTimestamp);
		amqpProperties.setType("test.type");
		amqpProperties.setReceivedUserId("test.userId");
		amqpProperties.setHeader(AmqpHeaders.SPRING_REPLY_CORRELATION, "test.correlation");
		amqpProperties.setHeader(AmqpHeaders.SPRING_REPLY_TO_STACK, "test.replyTo2");
		Map<String, Object> headerMap = headerMapper.toHeadersFromReply(amqpProperties);
		assertEquals("test.appId", headerMap.get(AmqpHeaders.APP_ID));
		assertEquals("test.clusterId", headerMap.get(AmqpHeaders.CLUSTER_ID));
		assertEquals("test.contentEncoding", headerMap.get(AmqpHeaders.CONTENT_ENCODING));
		assertEquals(99L, headerMap.get(AmqpHeaders.CONTENT_LENGTH));
		assertEquals("test.contentType", headerMap.get(AmqpHeaders.CONTENT_TYPE));
		assertEquals(testCorrelationId, headerMap.get(AmqpHeaders.CORRELATION_ID));
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, headerMap.get(AmqpHeaders.RECEIVED_DELIVERY_MODE));
		assertEquals(1234L, headerMap.get(AmqpHeaders.DELIVERY_TAG));
		assertEquals("test.expiration", headerMap.get(AmqpHeaders.EXPIRATION));
		assertEquals(42, headerMap.get(AmqpHeaders.MESSAGE_COUNT));
		assertEquals("test.messageId", headerMap.get(AmqpHeaders.MESSAGE_ID));
		assertEquals(4567, headerMap.get(AmqpHeaders.RECEIVED_DELAY));
		assertEquals("test.receivedExchange", headerMap.get(AmqpHeaders.RECEIVED_EXCHANGE));
		assertEquals("test.receivedRoutingKey", headerMap.get(AmqpHeaders.RECEIVED_ROUTING_KEY));
		assertEquals("test.replyTo", headerMap.get(AmqpHeaders.REPLY_TO));
		assertEquals(testTimestamp, headerMap.get(AmqpHeaders.TIMESTAMP));
		assertEquals("test.type", headerMap.get(AmqpHeaders.TYPE));
		assertEquals("test.userId", headerMap.get(AmqpHeaders.RECEIVED_USER_ID));
		assertEquals("test.correlation", headerMap.get(AmqpHeaders.SPRING_REPLY_CORRELATION));
		assertEquals("test.replyTo2", headerMap.get(AmqpHeaders.SPRING_REPLY_TO_STACK));
	}

	@Test
	public void testToHeadersConsumerMetadata() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setConsumerTag("consumerTag");
		amqpProperties.setConsumerQueue("consumerQueue");
		Map<String, Object> headerMap = headerMapper.toHeadersFromRequest(amqpProperties);
		assertEquals("consumerTag", headerMap.get(AmqpHeaders.CONSUMER_TAG));
		assertEquals("consumerQueue", headerMap.get(AmqpHeaders.CONSUMER_QUEUE));
	}

	@Test
	public void messageIdNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MessageHeaders.ID, "msg-id");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);
		assertNull(amqpProperties.getHeaders().get(MessageHeaders.ID));
	}

	@Test
	public void messageTimestampNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MessageHeaders.TIMESTAMP, 1234L);
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);
		assertNull(amqpProperties.getHeaders().get(MessageHeaders.TIMESTAMP));
	}

	@Test // INT-2090
	public void jsonTypeIdNotOverwritten() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageConverter converter = new Jackson2JsonMessageConverter();
		MessageProperties amqpProperties = new MessageProperties();
		converter.toMessage("123", amqpProperties);
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("__TypeId__", "java.lang.Integer");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);
		assertEquals("java.lang.String", amqpProperties.getHeaders().get("__TypeId__"));
		Object result = converter.fromMessage(new Message("123".getBytes(), amqpProperties));
		assertEquals(String.class, result.getClass());
	}

	@Test
	public void inboundOutbound() {
		DefaultAmqpHeaderMapper mapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
		amqpProperties.getHeaders().put("foo", "bar");
		amqpProperties.getHeaders().put("x-foo", "bar");
		Map<String, Object> headers = mapper.toHeadersFromRequest(amqpProperties);
		assertNull(headers.get(AmqpHeaders.DELIVERY_MODE));
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, headers.get(AmqpHeaders.RECEIVED_DELIVERY_MODE));
		assertEquals("bar", headers.get("foo"));
		assertEquals("bar", headers.get("x-foo"));

		amqpProperties = new MessageProperties();
		headers.put(AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.NON_PERSISTENT);
		mapper.fromHeadersToReply(new MessageHeaders(headers), amqpProperties);
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, amqpProperties.getDeliveryMode());
		assertEquals("bar", amqpProperties.getHeaders().get("foo"));
		assertNull(amqpProperties.getHeaders().get("x-foo"));

		mapper = DefaultAmqpHeaderMapper.outboundMapper();
		mapper.fromHeadersToRequest(new MessageHeaders(headers), amqpProperties);
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, amqpProperties.getDeliveryMode());
		assertEquals("bar", amqpProperties.getHeaders().get("foo"));
		assertNull(amqpProperties.getHeaders().get("x-foo"));

		amqpProperties.setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
		amqpProperties.setHeader("x-death", "foo");
		headers = mapper.toHeadersFromReply(amqpProperties);
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, headers.get(AmqpHeaders.RECEIVED_DELIVERY_MODE));
		assertNull(headers.get(AmqpHeaders.DELIVERY_MODE));
		assertEquals("bar", headers.get("foo"));
		assertEquals("foo", headers.get("x-death"));
	}

}
