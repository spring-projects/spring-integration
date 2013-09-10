/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ExpressionEvaluatingMessageSourceIntegrationTests {

	private static final AtomicInteger counter = new AtomicInteger();


	@Test
	public void test() throws Exception {
		QueueChannel channel = new QueueChannel();
		String payloadExpression = "'test-' + T(org.springframework.integration.endpoint.ExpressionEvaluatingMessageSourceIntegrationTests).next()";
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		Map<String, Expression> headerExpressions = new HashMap<String, Expression>();
		headerExpressions.put("foo", new LiteralExpression("x"));
		headerExpressions.put("bar", new SpelExpressionParser().parseExpression("7 * 6"));
		ExpressionFactoryBean factoryBean = new ExpressionFactoryBean(payloadExpression);
		factoryBean.afterPropertiesSet();
		Expression expression = factoryBean.getObject();
		ExpressionEvaluatingMessageSource<Object> source = new ExpressionEvaluatingMessageSource<Object>(expression, Object.class);
		source.setBeanFactory(mock(BeanFactory.class));
		source.setHeaderExpressions(headerExpressions);
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setSource(source);
		adapter.setTaskScheduler(scheduler);
		adapter.setMaxMessagesPerPoll(3);
		adapter.setTrigger(new PeriodicTrigger(60000));
		adapter.setOutputChannel(channel);
		adapter.setErrorHandler(new ErrorHandler() {
			public void handleError(Throwable t) {
				throw new IllegalStateException("unexpected exception in test", t);
			}
		});
		adapter.start();
		List<Message<?>> messages = new ArrayList<Message<?>>();
		for (int i = 0; i < 3; i++) {
			messages.add(channel.receive(1000));
		}
		scheduler.destroy();
		Message<?> message1 = messages.get(0);
		assertEquals("test-1", message1.getPayload());
		assertEquals("x", message1.getHeaders().get("foo"));
		assertEquals(42, message1.getHeaders().get("bar"));
		Message<?> message2 = messages.get(1);
		assertEquals("test-2", message2.getPayload());
		assertEquals("x", message2.getHeaders().get("foo"));
		assertEquals(42, message2.getHeaders().get("bar"));
		Message<?> message3 = messages.get(2);
		assertEquals("test-3", message3.getPayload());
		assertEquals("x", message3.getHeaders().get("foo"));
		assertEquals(42, message3.getHeaders().get("bar"));
	}


	public static int next() {
		return counter.incrementAndGet();
	}

}
