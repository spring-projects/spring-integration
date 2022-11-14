/*
 * Copyright 2011-2022 the original author or authors.
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

package org.springframework.integration.monitor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Tareq Abedrabbo
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0.4
 */
@SpringJUnitConfig
@DirtiesContext
public class MessageMetricsAdviceTests {

	private BeanFactory beanFactory;

	private BeanDefinitionRegistry beanDefinitionRegistry;

	@Autowired
	MessageMetricsAdviceTests(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
	}

	@Test
	void exportAdvisedHandler() {
		DummyInterceptor interceptor = new DummyInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("handleMessage");

		MessageHandler handler = new DummyHandler();

		ProxyFactory factory = new ProxyFactory(handler);
		factory.addAdvisor(advisor);
		MessageHandler advised = (MessageHandler) factory.getProxy();

		this.beanDefinitionRegistry.registerBeanDefinition("test",
				BeanDefinitionBuilder.genericBeanDefinition(MessageHandler.class, () -> advised)
						.getRawBeanDefinition());

		MessageHandler exported = this.beanFactory.getBean("test", MessageHandler.class);
		exported.handleMessage(MessageBuilder.withPayload("test").build());

		this.beanDefinitionRegistry.removeBeanDefinition("test");
	}

	@Test
	void exportAdvisedChannel() {
		DummyInterceptor interceptor = new DummyInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("send");

		MessageChannel channel = new NullChannel();

		ProxyFactory factory = new ProxyFactory(channel);
		factory.addAdvisor(advisor);
		MessageChannel advised = (MessageChannel) factory.getProxy();

		this.beanDefinitionRegistry.registerBeanDefinition("test",
				BeanDefinitionBuilder.genericBeanDefinition(MessageChannel.class, () -> advised)
						.getRawBeanDefinition());

		MessageChannel exported = this.beanFactory.getBean("test", MessageChannel.class);
		exported.send(MessageBuilder.withPayload("test").build());

		this.beanDefinitionRegistry.removeBeanDefinition("test");
	}

	private static class DummyHandler implements MessageHandler {

		@SuppressWarnings("unused")
		boolean invoked = false;

		DummyHandler() {
			super();
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			invoked = true;
		}

	}

	private static class DummyInterceptor implements MethodInterceptor {

		boolean invoked = false;

		DummyInterceptor() {
			super();
		}

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			invoked = true;
			return invocation.proceed();
		}

		@Override
		public String toString() {
			return super.toString() + "{" + "invoked=" + invoked + '}';
		}

	}

	@Configuration
	@EnableIntegrationMBeanExport
	@EnableIntegration
	public static class Config {

		@Bean
		public MBeanServerFactoryBean fb() {
			return new MBeanServerFactoryBean();
		}

	}

}
