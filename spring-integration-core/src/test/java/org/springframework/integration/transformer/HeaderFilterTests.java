/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.transformer;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class HeaderFilterTests {

	@Test
	public void testFilterDirectly() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("x", 1).setHeader("y", 2).setHeader("z", 3)
				.build();
		HeaderFilter filter = new HeaderFilter("x", "z");
		Message<?> result = filter.transform(message);
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("y")).isNotNull();
		assertThat(result.getHeaders().get("x")).isNull();
		assertThat(result.getHeaders().get("z")).isNull();
	}

	@Test
	public void testFilterWithinHandler() {
		UUID correlationId = UUID.randomUUID();
		QueueChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("x", 1).setHeader("y", 2).setHeader("z", 3)
				.setCorrelationId(correlationId)
				.setReplyChannel(replyChannel)
				.setErrorChannelName("testErrorChannel")
				.build();
		HeaderFilter filter = new HeaderFilter("x", "z");
		MessageTransformingHandler handler = new MessageTransformingHandler(filter);
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();
		handler.handleMessage(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("y")).isNotNull();
		assertThat(result.getHeaders().get("x")).isNull();
		assertThat(result.getHeaders().get("z")).isNull();
		assertThat(result.getHeaders().getErrorChannel()).isEqualTo("testErrorChannel");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
		assertThat(new IntegrationMessageHeaderAccessor(result).getCorrelationId()).isEqualTo(correlationId);
	}

	@Test
	public void testIdHeaderRemoval() {
		HeaderFilter filter = new HeaderFilter("foo", MessageHeaders.ID);
		try {
			filter.afterPropertiesSet();
			fail("BeanInitializationException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(BeanInitializationException.class);
			assertThat(e.getMessage()).contains("HeaderFilter cannot remove 'id' and 'timestamp' read-only headers.");
		}
	}

	@Test
	public void testTimestampHeaderRemoval() {
		HeaderFilter filter = new HeaderFilter(MessageHeaders.TIMESTAMP);
		try {
			filter.afterPropertiesSet();
			fail("BeanInitializationException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(BeanInitializationException.class);
			assertThat(e.getMessage()).contains("HeaderFilter cannot remove 'id' and 'timestamp' read-only headers.");
		}
	}

	@Test
	public void testIdPatternRemoval() {
		HeaderFilter filter = new HeaderFilter("*", MessageHeaders.ID);
		filter.setPatternMatch(true);
		try {
			filter.afterPropertiesSet();
			fail("BeanInitializationException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(BeanInitializationException.class);
			assertThat(e.getMessage()).contains("HeaderFilter cannot remove 'id' and 'timestamp' read-only headers.");
		}
	}

	@Test
	public void testPatternRemoval() {
		HeaderFilter filter = new HeaderFilter("time*");
		filter.setPatternMatch(true);

		filter.afterPropertiesSet();
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("time", new Date())
				.build();

		Message<?> result = filter.transform(message);

		assertThat(result.getHeaders())
				.containsKey(MessageHeaders.TIMESTAMP)
				.doesNotContainKey("time");
	}

}
