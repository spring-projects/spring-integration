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

package org.springframework.integration.jpa.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.outbound.JpaOutboundGateway;
import org.springframework.integration.jpa.support.OutboundGatewayType;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class JpaOutboundGatewayParserTests extends AbstractRequestHandlerAdvice {

	@Autowired
	private ConfigurableApplicationContext context;

	private EventDrivenConsumer consumer;

	@Test
	public void testRetrievingJpaOutboundGatewayParser() {
		setUp("retrievingJpaOutboundGateway");
		final AbstractMessageChannel inputChannel =
				TestUtils.getPropertyValue(this.consumer, "inputChannel", AbstractMessageChannel.class);
		assertThat(inputChannel.getComponentName()).isEqualTo("in");
		final JpaOutboundGateway jpaOutboundGateway =
				TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		final OutboundGatewayType gatewayType =
				TestUtils.getPropertyValue(jpaOutboundGateway, "gatewayType", OutboundGatewayType.class);
		assertThat(gatewayType).isEqualTo(OutboundGatewayType.RETRIEVING);
		long sendTimeout = TestUtils.getPropertyValue(jpaOutboundGateway, "messagingTemplate.sendTimeout", Long.class);
		assertThat(sendTimeout).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(jpaOutboundGateway, "requiresReply", Boolean.class)).isFalse();
		final JpaExecutor jpaExecutor =
				TestUtils.getPropertyValue(this.consumer, "handler.jpaExecutor", JpaExecutor.class);
		assertThat(jpaExecutor).isNotNull();
		final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);
		assertThat(entityClass.getName()).isEqualTo("org.springframework.integration.jpa.test.entity.StudentDomain");
		final JpaOperations jpaOperations =
				TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);
		assertThat(jpaOperations).isNotNull();
		assertThat(TestUtils.getPropertyValue(jpaExecutor, "expectSingleResult", Boolean.class)).isTrue();
		final LiteralExpression maxResultsExpression =
				TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression", LiteralExpression.class);
		assertThat(maxResultsExpression).isNotNull();
		assertThat(TestUtils.getPropertyValue(maxResultsExpression, "literalValue")).isEqualTo("55");

		assertThat(TestUtils.getPropertyValue(jpaExecutor, "deleteAfterPoll", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(jpaExecutor, "flush", Boolean.class)).isTrue();
	}

	@Test
	public void testRetrievingJpaOutboundGatewayParserWithFirstResult() {
		setUp("retrievingJpaOutboundGatewayWithFirstResult");
		final JpaOutboundGateway jpaOutboundGateway =
				TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		Expression firstResultExpression =
				TestUtils.getPropertyValue(jpaOutboundGateway, "jpaExecutor.firstResultExpression", Expression.class);
		assertThat(firstResultExpression).isNotNull();
		assertThat(firstResultExpression.getClass()).isEqualTo(LiteralExpression.class);
		assertThat(TestUtils.getPropertyValue(firstResultExpression, "literalValue", String.class)).isEqualTo("1");
	}

	@Test
	public void testRetrievingJpaOutboundGatewayParserWithFirstResultExpression() {
		setUp("retrievingJpaOutboundGatewayWithFirstResultExpression");
		final JpaOutboundGateway jpaOutboundGateway =
				TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		Expression firstResultExpression =
				TestUtils.getPropertyValue(jpaOutboundGateway, "jpaExecutor.firstResultExpression", Expression.class);
		assertThat(firstResultExpression).isNotNull();
		assertThat(firstResultExpression.getClass()).isEqualTo(SpelExpression.class);
		assertThat(TestUtils.getPropertyValue(firstResultExpression, "expression", String.class))
				.isEqualTo("header['firstResult']");
	}

	@Test
	public void testRetrievingJpaOutboundGatewayParserWithMaxResultExpression() {
		setUp("retrievingJpaOutboundGatewayWithMaxResultExpression");
		final JpaOutboundGateway jpaOutboundGateway =
				TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		Expression maxNumberOfResultExpression =
				TestUtils.getPropertyValue(jpaOutboundGateway, "jpaExecutor.maxResultsExpression", Expression.class);
		assertThat(maxNumberOfResultExpression).isNotNull();
		assertThat(maxNumberOfResultExpression.getClass()).isEqualTo(SpelExpression.class);
		assertThat(TestUtils.getPropertyValue(maxNumberOfResultExpression, "expression", String.class))
				.isEqualTo("header['maxResults']");
	}

	@Test
	public void testUpdatingJpaOutboundGatewayParser() {
		setUp("updatingJpaOutboundGateway");

		AbstractMessageChannel inputChannel =
				TestUtils.getPropertyValue(this.consumer, "inputChannel", AbstractMessageChannel.class);

		assertThat(inputChannel.getComponentName()).isEqualTo("in");

		final JpaOutboundGateway jpaOutboundGateway =
				TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);

		final OutboundGatewayType gatewayType =
				TestUtils.getPropertyValue(jpaOutboundGateway, "gatewayType", OutboundGatewayType.class);

		assertThat(gatewayType).isEqualTo(OutboundGatewayType.UPDATING);

		long sendTimeout = TestUtils.getPropertyValue(jpaOutboundGateway, "messagingTemplate.sendTimeout", Long.class);

		assertThat(sendTimeout).isEqualTo(100);

		assertThat(TestUtils.getPropertyValue(jpaOutboundGateway, "requiresReply", Boolean.class)).isFalse();

		final JpaExecutor jpaExecutor =
				TestUtils.getPropertyValue(this.consumer, "handler.jpaExecutor", JpaExecutor.class);

		assertThat(jpaExecutor).isNotNull();

		final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertThat(entityClass.getName()).isEqualTo("org.springframework.integration.jpa.test.entity.StudentDomain");

		JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertThat(jpaOperations).isNotNull();

		Boolean usePayloadAsParameterSource =
				TestUtils.getPropertyValue(jpaExecutor, "usePayloadAsParameterSource", Boolean.class);

		assertThat(usePayloadAsParameterSource).isTrue();

		final Integer order = TestUtils.getPropertyValue(jpaOutboundGateway, "order", Integer.class);

		assertThat(order).isEqualTo(Integer.valueOf(2));

		final PersistMode persistMode = TestUtils.getPropertyValue(jpaExecutor, "persistMode", PersistMode.class);

		assertThat(persistMode).isEqualTo(PersistMode.PERSIST);

		assertThat(TestUtils.getPropertyValue(jpaExecutor, "flushSize", Integer.class)).isEqualTo(Integer.valueOf(100));
		assertThat(TestUtils.getPropertyValue(jpaExecutor, "clearOnFlush", Boolean.class)).isTrue();
	}

	@Test
	public void advised() {
		setUp("advised");
		EventDrivenConsumer jpaOutboundGatewayEndpoint = context.getBean("advised", EventDrivenConsumer.class);
		MessageHandler jpaOutboundGateway =
				TestUtils.getPropertyValue(jpaOutboundGatewayEndpoint, "handler", MessageHandler.class);
		FooAdvice advice = context.getBean("jpaFooAdvice", FooAdvice.class);
		assertThat(AopUtils.isAopProxy(jpaOutboundGateway)).isTrue();

		assertThatExceptionOfType(ReplyRequiredException.class)
				.isThrownBy(() -> jpaOutboundGateway.handleMessage(new GenericMessage<>("foo")));

		verify(advice).doInvoke(any(ExecutionCallback.class), any(Object.class), any(Message.class));
	}

	@Test
	public void testJpaExecutorBeanIdNaming() {
		assertThat(context.getBean("retrievingJpaOutboundGateway.jpaExecutor", JpaExecutor.class)).isNotNull();
		assertThat(context.getBean("updatingJpaOutboundGateway.jpaExecutor", JpaExecutor.class)).isNotNull();
	}

	@Test
	public void withBothFirstResultAndFirstResultExpressionPresent() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("JpaInvalidOutboundGatewayParserTests.xml", getClass()))
				.withMessageStartingWith(
						"Configuration problem: Only one of 'first-result' or 'first-result-expression' is allowed");
	}

	private void setUp(String gatewayId) {
		consumer = this.context.getBean(gatewayId, EventDrivenConsumer.class);
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		// Workaround for access to protected AbstractRequestHandlerAdvice.ExecutionCallback
		return null;
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			return null;
		}

	}

}
