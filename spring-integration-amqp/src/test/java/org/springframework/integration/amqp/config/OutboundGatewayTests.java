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

package org.springframework.integration.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class OutboundGatewayTests {

	private final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());

	@Test
	public void testVanillaConfiguration() throws Exception {
		assertTrue(context.getBeanFactory().containsBeanDefinition("vanilla"));
		context.getBean("vanilla");
	}

	@Test
	public void testExpressionBasedConfiguration() throws Exception {
		assertTrue(context.getBeanFactory().containsBeanDefinition("expression"));
		Object target = context.getBean("expression");
		assertNotNull(ReflectionTestUtils.getField(ReflectionTestUtils.getField(target, "handler"), "routingKeyGenerator"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testExpressionsBeanResolver() throws Exception {
		ApplicationContext context = mock(ApplicationContext.class);
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return invocation.getArguments()[0] + "bar";
			}
		}).when(context).getBean(anyString());
		when(context.containsBean(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)).thenReturn(true);
		when(context.getBean(ClassUtils.forName("org.springframework.integration.config.SpelPropertyAccessorRegistrar",
				context.getClassLoader())))
				.thenThrow(NoSuchBeanDefinitionException.class);
		IntegrationEvaluationContextFactoryBean integrationEvaluationContextFactoryBean = new IntegrationEvaluationContextFactoryBean();
		integrationEvaluationContextFactoryBean.setApplicationContext(context);
		integrationEvaluationContextFactoryBean.afterPropertiesSet();
		StandardEvaluationContext evalContext = integrationEvaluationContextFactoryBean.getObject();
		when(context.getBean(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME, StandardEvaluationContext.class))
			.thenReturn(evalContext);
		RabbitTemplate template = mock(RabbitTemplate.class);
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(template);
		endpoint.setRoutingKeyExpression("@foo");
		endpoint.setExchangeNameExpression("@bar");
		endpoint.setConfirmCorrelationExpression("@baz");
		endpoint.setBeanFactory(context);
		endpoint.afterPropertiesSet();
		Message<?> message = new GenericMessage<String>("Hello, world!");
		assertEquals("foobar", TestUtils.getPropertyValue(endpoint, "routingKeyGenerator", MessageProcessor.class)
				.processMessage(message));
		assertEquals("barbar", TestUtils.getPropertyValue(endpoint, "exchangeNameGenerator", MessageProcessor.class)
				.processMessage(message));
		assertEquals("bazbar", TestUtils.getPropertyValue(endpoint, "correlationDataGenerator", MessageProcessor.class)
				.processMessage(message));
	}

}
