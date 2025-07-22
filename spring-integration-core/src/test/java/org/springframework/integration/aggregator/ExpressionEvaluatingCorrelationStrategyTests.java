/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.aggregator;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 */
public class ExpressionEvaluatingCorrelationStrategyTests implements TestApplicationContextAware {

	private ExpressionEvaluatingCorrelationStrategy strategy;

	@Test
	public void testCreateInstanceWithEmptyExpressionFails() {
		assertThatThrownBy(() -> strategy = new ExpressionEvaluatingCorrelationStrategy(""))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testCreateInstanceWithNullExpressionFails() {
		Expression nullExpression = null;
		assertThatThrownBy(() -> strategy = new ExpressionEvaluatingCorrelationStrategy(nullExpression))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testCorrelationKeyWithMethodInvokingExpression() throws Exception {
		ExpressionParser parser = new SpelExpressionParser(new SpelParserConfiguration(true, true));
		Expression expression = parser.parseExpression("payload.substring(0,1)");
		strategy = new ExpressionEvaluatingCorrelationStrategy(expression);
		strategy.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Object correlationKey = strategy.getCorrelationKey(new GenericMessage<String>("bla"));
		assertThat(correlationKey).isInstanceOf(String.class);
		assertThat((String) correlationKey).isEqualTo("b");
	}

	@Test
	public void testCorrelationStrategyWithAtBeanExpression() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("expression-evaluating-correlation-with-bf.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel outputChannel = context.getBean("outputChannel", QueueChannel.class);
		Message<?> message = MessageBuilder.withPayload("foo").setSequenceNumber(1).setSequenceSize(1).build();
		inputChannel.send(message);
		Message<?> reply = outputChannel.receive(0);
		assertThat(reply).isNotNull();
		context.close();
	}

	public static class CustomCorrelator {

		public Object correlate(Object o) {
			return o;
		}

	}

}
