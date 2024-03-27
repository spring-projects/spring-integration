/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ExpressionEvaluatingMessageSourceIntegrationTests {

	private static final AtomicInteger counter = new AtomicInteger();

	@Test
	public void test() throws Exception {
		QueueChannel channel = new QueueChannel();
		String payloadExpression =
				"'test-' + T(org.springframework.integration.endpoint.ExpressionEvaluatingMessageSourceIntegrationTests).next()";
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		Map<String, Expression> headerExpressions = new HashMap<>();
		headerExpressions.put("foo", new LiteralExpression("x"));
		headerExpressions.put("bar", new SpelExpressionParser().parseExpression("7 * 6"));
		ExpressionFactoryBean factoryBean = new ExpressionFactoryBean(payloadExpression);
		factoryBean.afterPropertiesSet();
		Expression expression = factoryBean.getObject();
		ExpressionEvaluatingMessageSource<Object> source =
				new ExpressionEvaluatingMessageSource<>(expression, Object.class);
		source.setBeanFactory(mock(BeanFactory.class));
		source.setHeaderExpressions(headerExpressions);
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setSource(source);
		adapter.setTaskScheduler(scheduler);
		adapter.setMaxMessagesPerPoll(3);
		adapter.setTrigger(new PeriodicTrigger(Duration.ofSeconds(60)));
		adapter.setOutputChannel(channel);
		adapter.setErrorHandler(t -> {
			throw new IllegalStateException("unexpected exception in test", t);
		});
		adapter.start();
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			messages.add(channel.receive(1000));
		}
		adapter.stop();
		scheduler.destroy();
		Message<?> message1 = messages.get(0);
		assertThat(message1.getPayload()).isEqualTo("test-1");
		assertThat(message1.getHeaders().get("foo")).isEqualTo("x");
		assertThat(message1.getHeaders().get("bar")).isEqualTo(42);
		Message<?> message2 = messages.get(1);
		assertThat(message2.getPayload()).isEqualTo("test-2");
		assertThat(message2.getHeaders().get("foo")).isEqualTo("x");
		assertThat(message2.getHeaders().get("bar")).isEqualTo(42);
		Message<?> message3 = messages.get(2);
		assertThat(message3.getPayload()).isEqualTo("test-3");
		assertThat(message3.getHeaders().get("foo")).isEqualTo("x");
		assertThat(message3.getHeaders().get("bar")).isEqualTo(42);
	}

	public static int next() {
		return counter.incrementAndGet();
	}

}
