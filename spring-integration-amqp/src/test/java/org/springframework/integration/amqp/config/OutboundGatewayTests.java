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

package org.springframework.integration.amqp.config;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.SpelPropertyAccessorRegistrar;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class OutboundGatewayTests {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	@Autowired
	ConfigurableApplicationContext context;

	@Test
	public void testVanillaConfiguration() {
		assertThat(context.getBeanFactory().containsBeanDefinition("vanilla")).isTrue();
		context.getBean("vanilla");
	}

	@Test
	public void testExpressionBasedConfiguration() {
		assertThat(context.getBeanFactory().containsBeanDefinition("expression")).isTrue();
		Object target = context.getBean("expression");
		assertThat(ReflectionTestUtils.getField(ReflectionTestUtils.getField(target, "handler"),
				"routingKeyGenerator")).isNotNull();
	}

	@Test
	public void testExpressionsBeanResolver() {
		ApplicationContext context = mock(ApplicationContext.class);
		doAnswer(invocation -> invocation.getArguments()[0] + "bar").when(context).getBean(anyString());
		when(context.containsBean(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)).thenReturn(true);
		when(context.getBean(SpelPropertyAccessorRegistrar.class))
				.thenThrow(NoSuchBeanDefinitionException.class);
		IntegrationEvaluationContextFactoryBean integrationEvaluationContextFactoryBean =
				new IntegrationEvaluationContextFactoryBean();
		integrationEvaluationContextFactoryBean.setApplicationContext(context);
		integrationEvaluationContextFactoryBean.afterPropertiesSet();
		StandardEvaluationContext evalContext = integrationEvaluationContextFactoryBean.getObject();
		when(context.getBean(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
				StandardEvaluationContext.class))
				.thenReturn(evalContext);
		RabbitTemplate template = spy(new RabbitTemplate());
		willReturn(mock(ConnectionFactory.class)).given(template).getConnectionFactory();
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(template);
		endpoint.setRoutingKeyExpression(PARSER.parseExpression("@foo"));
		endpoint.setExchangeNameExpression(PARSER.parseExpression("@bar"));
		endpoint.setConfirmCorrelationExpressionString("@baz");
		endpoint.setBeanFactory(context);
		endpoint.afterPropertiesSet();
		Message<?> message = new GenericMessage<>("Hello, world!");
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(endpoint, "routingKeyGenerator")
				.processMessage(message)).isEqualTo("foobar");
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(endpoint, "exchangeNameGenerator")
				.processMessage(message)).isEqualTo("barbar");
		assertThat(TestUtils.<MessageProcessor<?>>getPropertyValue(endpoint, "correlationDataGenerator")
				.processMessage(message)).isEqualTo("bazbar");
	}

}
