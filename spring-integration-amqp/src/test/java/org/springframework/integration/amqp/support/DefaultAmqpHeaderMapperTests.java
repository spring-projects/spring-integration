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

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.JsonMessageConverter;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.amqp.AmqpHeaders;

/**
 * @author Mark Fisher
 * @since 2.1
 */
public class DefaultAmqpHeaderMapperTests {

	@Test
	public void fromHeaders() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper(true);
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(AmqpHeaders.APP_ID, "test.appId");
		headerMap.put(AmqpHeaders.CLUSTER_ID, "test.clusterId");
		headerMap.put(AmqpHeaders.CONTENT_ENCODING, "test.contentEncoding");
		headerMap.put(AmqpHeaders.CONTENT_LENGTH, 99L);
		headerMap.put(AmqpHeaders.CONTENT_TYPE, "test.contentType");
		byte[] testCorrelationId = new byte[] {1,2,3};
		headerMap.put(AmqpHeaders.CORRELATION_ID, testCorrelationId);
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
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeaders(integrationHeaders, amqpProperties);
		assertEquals("test.appId", amqpProperties.getAppId());
		assertEquals("test.clusterId", amqpProperties.getClusterId());
		assertEquals("test.contentEncoding", amqpProperties.getContentEncoding());
		assertEquals(99L, amqpProperties.getContentLength());
		assertEquals("test.contentType", amqpProperties.getContentType());
		assertEquals(testCorrelationId, amqpProperties.getCorrelationId());
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
	}

	@Test
	public void toHeaders() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper(true);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		byte[] testCorrelationId = new byte[] {1,2,3};
		amqpProperties.setCorrelationId(testCorrelationId);
		amqpProperties.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
		amqpProperties.setDeliveryTag(1234L);
		amqpProperties.setExpiration("test.expiration");
		amqpProperties.setMessageCount(42);
		amqpProperties.setMessageId("test.messageId");
		amqpProperties.setPriority(22);
		amqpProperties.setReceivedExchange("test.receivedExchange");
		amqpProperties.setReceivedRoutingKey("test.receivedRoutingKey");
		amqpProperties.setRedelivered(true);
		amqpProperties.setReplyTo("test.replyTo");
		Date testTimestamp = new Date();
		amqpProperties.setTimestamp(testTimestamp);
		amqpProperties.setType("test.type");
		amqpProperties.setUserId("test.userId");
		Map<String, Object> headerMap = headerMapper.toHeaders(amqpProperties);
		assertEquals("test.appId", headerMap.get(AmqpHeaders.APP_ID));
		assertEquals("test.clusterId", headerMap.get(AmqpHeaders.CLUSTER_ID));
		assertEquals("test.contentEncoding", headerMap.get(AmqpHeaders.CONTENT_ENCODING));
		assertEquals(99L, headerMap.get(AmqpHeaders.CONTENT_LENGTH));
		assertEquals("test.contentType", headerMap.get(AmqpHeaders.CONTENT_TYPE));
		assertEquals(testCorrelationId, headerMap.get(AmqpHeaders.CORRELATION_ID));
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, headerMap.get(AmqpHeaders.DELIVERY_MODE));
		assertEquals(1234L, headerMap.get(AmqpHeaders.DELIVERY_TAG));
		assertEquals("test.expiration", headerMap.get(AmqpHeaders.EXPIRATION));
		assertEquals(new Integer(42), headerMap.get(AmqpHeaders.MESSAGE_COUNT));
		assertEquals("test.messageId", headerMap.get(AmqpHeaders.MESSAGE_ID));
		assertEquals("test.receivedExchange", headerMap.get(AmqpHeaders.RECEIVED_EXCHANGE));
		assertEquals("test.receivedRoutingKey", headerMap.get(AmqpHeaders.RECEIVED_ROUTING_KEY));
		assertEquals("test.replyTo", headerMap.get(AmqpHeaders.REPLY_TO));
		assertEquals(testTimestamp, headerMap.get(AmqpHeaders.TIMESTAMP));
		assertEquals("test.type", headerMap.get(AmqpHeaders.TYPE));
		assertEquals("test.userId", headerMap.get(AmqpHeaders.USER_ID));
	}

	@Test
	public void replyChannelNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper(true);
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MessageHeaders.REPLY_CHANNEL, "foo");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeaders(integrationHeaders, amqpProperties);
		assertEquals(null, amqpProperties.getHeaders().get(MessageHeaders.REPLY_CHANNEL));
	}

	@Test
	public void errorChannelNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper(true);
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MessageHeaders.ERROR_CHANNEL, "foo");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeaders(integrationHeaders, amqpProperties);
		assertEquals(null, amqpProperties.getHeaders().get(MessageHeaders.ERROR_CHANNEL));
	}

	@Test // INT-2090
	public void jsonTypeIdNotOverwritten() {
		DefaultAmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper(true);
		JsonMessageConverter converter = new JsonMessageConverter();
		MessageProperties amqpProperties = new MessageProperties();
		converter.toMessage("123", amqpProperties);
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("__TypeId__", "java.lang.Integer");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		headerMapper.fromHeaders(integrationHeaders, amqpProperties);
		assertEquals("java.lang.String", amqpProperties.getHeaders().get("__TypeId__"));
		Object result = converter.fromMessage(new Message("123".getBytes(), amqpProperties));
		assertEquals(String.class, result.getClass());
	}
}
