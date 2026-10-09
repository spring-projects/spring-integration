/*
 * Copyright 2002-present the original author or authors.
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
 * @author Glenn Renfro
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
	public void testCorrelationKeyWithMethodInvokingExpression() {
		ExpressionParser parser = new SpelExpressionParser(SpelParserConfiguration.builder().autoGrowNullReferences()
				.autoGrowCollections().build());
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
