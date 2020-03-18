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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.integration.mapping.support.JsonHeaders;
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
 * @author Steve Singer
 *
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
		Map<String, Object> headerMap = new HashMap<>();
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
		assertThat(amqpProperties.getAppId()).isEqualTo("test.appId");
		assertThat(amqpProperties.getClusterId()).isEqualTo("test.clusterId");
		assertThat(amqpProperties.getContentEncoding()).isEqualTo("test.contentEncoding");
		assertThat(amqpProperties.getContentLength()).isEqualTo(99L);
		assertThat(amqpProperties.getContentType()).isEqualTo("test.contentType");
		assertThat(amqpProperties.getCorrelationId()).isEqualTo(testCorrelationId);
		assertThat(amqpProperties.getDelay()).isEqualTo(Integer.valueOf(1234));
		assertThat(amqpProperties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(amqpProperties.getDeliveryTag()).isEqualTo(1234L);
		assertThat(amqpProperties.getExpiration()).isEqualTo("test.expiration");
		assertThat(amqpProperties.getMessageCount()).isEqualTo(new Integer(42));
		assertThat(amqpProperties.getMessageId()).isEqualTo("test.messageId");
		assertThat(amqpProperties.getReceivedExchange()).isEqualTo("test.receivedExchange");
		assertThat(amqpProperties.getReceivedRoutingKey()).isEqualTo("test.receivedRoutingKey");
		assertThat(amqpProperties.getReplyTo()).isEqualTo("test.replyTo");
		assertThat(amqpProperties.getTimestamp()).isEqualTo(testTimestamp);
		assertThat(amqpProperties.getType()).isEqualTo("test.type");
		assertThat(amqpProperties.getUserId()).isEqualTo("test.userId");

		assertThat(amqpProperties.getHeaders().get(MessageHeaders.ERROR_CHANNEL)).isNull();
		assertThat(amqpProperties.getHeaders().get(MessageHeaders.REPLY_CHANNEL)).isNull();
	}

	@Test
	public void fromHeadersWithContentTypeAsMediaType() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<>();

		MediaType contentType = MediaType.parseMediaType("text/html");
		headerMap.put(AmqpHeaders.CONTENT_TYPE, contentType);

		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);

		assertThat(amqpProperties.getContentType()).isEqualTo("text/html");

		headerMap.put(AmqpHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON);
		integrationHeaders = new MessageHeaders(headerMap);
		amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);

		assertThat(amqpProperties.getContentType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON_VALUE);
	}

	@Test
	public void fromHeadersWithContentTypeAsMimeType() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<>();

		MimeType contentType = MimeType.valueOf("text/html");
		headerMap.put(AmqpHeaders.CONTENT_TYPE, contentType);

		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);

		assertThat(amqpProperties.getContentType()).isEqualTo("text/html");
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
		assertThat(headerMap.get(AmqpHeaders.APP_ID)).isEqualTo("test.appId");
		assertThat(headerMap.get(AmqpHeaders.CLUSTER_ID)).isEqualTo("test.clusterId");
		assertThat(headerMap.get(AmqpHeaders.CONTENT_ENCODING)).isEqualTo("test.contentEncoding");
		assertThat(headerMap.get(AmqpHeaders.CONTENT_LENGTH)).isEqualTo(99L);
		assertThat(headerMap.get(AmqpHeaders.CONTENT_TYPE)).isEqualTo("test.contentType");
		assertThat(headerMap.get(AmqpHeaders.CORRELATION_ID)).isEqualTo(testCorrelationId);
		assertThat(headerMap.get(AmqpHeaders.RECEIVED_DELIVERY_MODE)).isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(headerMap.get(AmqpHeaders.DELIVERY_TAG)).isEqualTo(1234L);
		assertThat(headerMap.get(AmqpHeaders.EXPIRATION)).isEqualTo("test.expiration");
		assertThat(headerMap.get(AmqpHeaders.MESSAGE_COUNT)).isEqualTo(42);
		assertThat(headerMap.get(AmqpHeaders.MESSAGE_ID)).isEqualTo("test.messageId");
		assertThat(headerMap.get(AmqpHeaders.RECEIVED_DELAY)).isEqualTo(4567);
		assertThat(headerMap.get(AmqpHeaders.RECEIVED_EXCHANGE)).isEqualTo("test.receivedExchange");
		assertThat(headerMap.get(AmqpHeaders.RECEIVED_ROUTING_KEY)).isEqualTo("test.receivedRoutingKey");
		assertThat(headerMap.get(AmqpHeaders.REPLY_TO)).isEqualTo("test.replyTo");
		assertThat(headerMap.get(AmqpHeaders.TIMESTAMP)).isEqualTo(testTimestamp);
		assertThat(headerMap.get(AmqpHeaders.TYPE)).isEqualTo("test.type");
		assertThat(headerMap.get(AmqpHeaders.RECEIVED_USER_ID)).isEqualTo("test.userId");
		assertThat(headerMap.get(AmqpHeaders.SPRING_REPLY_CORRELATION)).isEqualTo("test.correlation");
		assertThat(headerMap.get(AmqpHeaders.SPRING_REPLY_TO_STACK)).isEqualTo("test.replyTo2");
	}

	@Test
	public void toHeadersNonContentType() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentType(null);
		String testCorrelationId = "foo";
		amqpProperties.setCorrelationId(testCorrelationId);
		Map<String, Object> headerMap = headerMapper.toHeadersFromReply(amqpProperties);
		assertThat(headerMap.get(AmqpHeaders.CORRELATION_ID)).isEqualTo(testCorrelationId);
	}


	@Test
	public void testToHeadersConsumerMetadata() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setConsumerTag("consumerTag");
		amqpProperties.setConsumerQueue("consumerQueue");
		Map<String, Object> headerMap = headerMapper.toHeadersFromRequest(amqpProperties);
		assertThat(headerMap.get(AmqpHeaders.CONSUMER_TAG)).isEqualTo("consumerTag");
		assertThat(headerMap.get(AmqpHeaders.CONSUMER_QUEUE)).isEqualTo("consumerQueue");
	}

	@Test
	public void messageIdNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put(MessageHeaders.ID, "msg-id");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);
		assertThat(amqpProperties.getHeaders().get(MessageHeaders.ID)).isNull();
	}

	@Test
	public void messageTimestampNotMappedToAmqpProperties() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put(MessageHeaders.TIMESTAMP, 1234L);
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);
		assertThat(amqpProperties.getHeaders().get(MessageHeaders.TIMESTAMP)).isNull();
	}

	@Test
	public void jsonTypeIdNotOverwritten() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageConverter converter = new Jackson2JsonMessageConverter();
		MessageProperties amqpProperties = new MessageProperties();
		converter.toMessage("123", amqpProperties);
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("__TypeId__", "java.lang.Integer");
		MessageHeaders integrationHeaders = new MessageHeaders(headerMap);
		headerMapper.fromHeadersToRequest(integrationHeaders, amqpProperties);
		assertThat(amqpProperties.getHeaders().get("__TypeId__")).isEqualTo("java.lang.String");
		Object result = converter.fromMessage(new Message("123".getBytes(), amqpProperties));
		assertThat(result.getClass()).isEqualTo(String.class);
	}

	@Test
	public void inboundOutbound() {
		DefaultAmqpHeaderMapper mapper = DefaultAmqpHeaderMapper.inboundMapper();
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
		amqpProperties.getHeaders().put("foo", "bar");
		amqpProperties.getHeaders().put("x-foo", "bar");
		Map<String, Object> headers = mapper.toHeadersFromRequest(amqpProperties);
		assertThat(headers.get(AmqpHeaders.DELIVERY_MODE)).isNull();
		assertThat(headers.get(AmqpHeaders.RECEIVED_DELIVERY_MODE)).isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(headers.get("foo")).isEqualTo("bar");
		assertThat(headers.get("x-foo")).isEqualTo("bar");

		amqpProperties = new MessageProperties();
		headers.put(AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.NON_PERSISTENT);
		mapper.fromHeadersToReply(new MessageHeaders(headers), amqpProperties);
		assertThat(amqpProperties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(amqpProperties.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(amqpProperties.getHeaders().get("x-foo")).isNull();

		mapper = DefaultAmqpHeaderMapper.outboundMapper();
		mapper.fromHeadersToRequest(new MessageHeaders(headers), amqpProperties);
		assertThat(amqpProperties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(amqpProperties.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(amqpProperties.getHeaders().get("x-foo")).isNull();

		amqpProperties.setReceivedDeliveryMode(MessageDeliveryMode.NON_PERSISTENT);
		amqpProperties.setHeader("x-death", "foo");
		headers = mapper.toHeadersFromReply(amqpProperties);
		assertThat(headers.get(AmqpHeaders.RECEIVED_DELIVERY_MODE)).isEqualTo(MessageDeliveryMode.NON_PERSISTENT);
		assertThat(headers.get(AmqpHeaders.DELIVERY_MODE)).isNull();
		assertThat(headers.get("foo")).isEqualTo("bar");
		assertThat(headers.get("x-death")).isEqualTo("foo");
	}

	@Test
	public void jsonHeadersResolvableTypeSkipped() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.outboundMapper();
		MessageHeaders integrationHeaders =
				new MessageHeaders(
						Collections.singletonMap(JsonHeaders.RESOLVABLE_TYPE, ResolvableType.forClass(String.class)));
		MessageProperties amqpProperties = new MessageProperties();
		headerMapper.fromHeadersToReply(integrationHeaders, amqpProperties);

		assertThat(amqpProperties.getHeaders()).doesNotContainKeys(JsonHeaders.RESOLVABLE_TYPE);
	}

	@Test
	public void jsonHeadersNotMapped() {
		DefaultAmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();
		headerMapper.setRequestHeaderNames("!json_*", "*");
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.getHeaders().put(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME, "test.type");
		Map<String, Object> headers = headerMapper.toHeadersFromRequest(amqpProperties);
		assertThat(headers)
				.doesNotContainKeys(JsonHeaders.RESOLVABLE_TYPE, JsonHeaders.TYPE_ID)
				.containsKey(AbstractJavaTypeMapper.DEFAULT_CLASSID_FIELD_NAME);
	}

}
