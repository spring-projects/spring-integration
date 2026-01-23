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

package org.springframework.integration.jms.config;

import jakarta.jms.DeliveryMode;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class JmsOutboundChannelAdapterParserTests {

	private static volatile int adviceCalled;

	@Test
	public void adapterWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithConnectionFactoryAndDestination.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertThat(accessor.getPropertyValue("jmsTemplate")).isNotNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.jmsTemplate.sessionTransacted"))
				.isTrue();
		context.close();
	}

	@Test
	public void advisedAdapter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithConnectionFactoryAndDestination.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("advised");
		MessageHandler handler = TestUtils.getPropertyValue(endpoint, "handler");
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
		context.close();
	}

	@Test
	public void adapterWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithConnectionFactoryAndDestinationName.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertThat(accessor.getPropertyValue("jmsTemplate")).isNotNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "handler.jmsTemplate.sessionTransacted"))
				.isFalse();
		context.close();
	}

	@Test
	public void adapterWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithDefaultConnectionFactory.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertThat(accessor.getPropertyValue("jmsTemplate")).isNotNull();
		context.close();
	}

	@Test
	public void adapterWithOrder() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithOrder.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		Object order = accessor.getPropertyValue("order");
		assertThat(order).isEqualTo(123);
		context.close();
	}

	@Test
	public void adapterWithHeaderMapper() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithHeaderMapper.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		JmsHeaderMapper headerMapper = (JmsHeaderMapper) accessor.getPropertyValue("headerMapper");
		assertThat(headerMapper).isNotNull();
		assertThat(headerMapper.getClass()).isEqualTo(TestJmsHeaderMapper.class);
		context.close();
	}

	@Test
	public void adapterWithJmsTemplate() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithJmsTemplate.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		JmsTemplate jmsTemplate = (JmsTemplate) handlerAccessor.getPropertyValue("jmsTemplate");
		assertThat(jmsTemplate).isNotNull();
		assertThat(jmsTemplate).isEqualTo(context.getBean("template"));
		context.close();
	}

	@Test
	public void adapterWithJmsTemplateQos() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithJmsTemplateQos.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		Object handler = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		JmsTemplate jmsTemplate = (JmsTemplate) handlerAccessor.getPropertyValue("jmsTemplate");
		assertThat(jmsTemplate).isNotNull();
		assertThat(jmsTemplate).isEqualTo(context.getBean("template"));
		assertThat(jmsTemplate.isExplicitQosEnabled()).isTrue();
		assertThat(jmsTemplate.getPriority()).isEqualTo(7);
		assertThat(jmsTemplate.getTimeToLive()).isEqualTo(12345);
		assertThat(TestUtils.<String>getPropertyValue(handler, "deliveryModeExpression.expression"))
				.isEqualTo("1");
		assertThat(TestUtils.<String>getPropertyValue(handler, "timeToLiveExpression.expression"))
				.isEqualTo("100");
		context.close();
	}

	@Test
	public void adapterWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithMessageConverter.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("adapter");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		JmsTemplate jmsTemlate = (JmsTemplate) handlerAccessor.getPropertyValue("jmsTemplate");
		MessageConverter messageConverter = jmsTemlate.getMessageConverter();
		assertThat(messageConverter).isNotNull();
		assertThat(messageConverter.getClass()).isEqualTo(TestMessageConverter.class);
		context.close();
	}

	@Test
	public void adapterWithEmptyConnectionFactory() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> new ClassPathXmlApplicationContext("jmsOutboundWithEmptyConnectionFactory.xml", this.getClass()))
				.withMessageContaining("connection-factory");
	}

	@Test
	public void adapterWithQosSettings() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithQos.xml", this.getClass());
		EventDrivenConsumer endpoint = context.getBean("qosAdapter", EventDrivenConsumer.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(new DirectFieldAccessor(endpoint).getPropertyValue("handler"))
						.getPropertyValue("jmsTemplate"));
		assertThat(accessor.getPropertyValue("explicitQosEnabled")).isEqualTo(true);
		assertThat(accessor.getPropertyValue("timeToLive")).isEqualTo(12345L);
		assertThat(accessor.getPropertyValue("priority")).isEqualTo(7);
		assertThat(accessor.getPropertyValue("deliveryMode")).isEqualTo(DeliveryMode.NON_PERSISTENT);
		context.close();
	}

	@Test
	public void qosNotExplicitByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundWithQos.xml", this.getClass());
		EventDrivenConsumer endpoint = context.getBean("defaultAdapter", EventDrivenConsumer.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(new DirectFieldAccessor(endpoint).getPropertyValue("handler"))
						.getPropertyValue("jmsTemplate"));
		assertThat(accessor.getPropertyValue("explicitQosEnabled")).isEqualTo(false);
		context.close();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
