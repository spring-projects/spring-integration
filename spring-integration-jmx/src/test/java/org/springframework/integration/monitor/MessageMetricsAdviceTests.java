/*
 * Copyright 2011-2016 the original author or authors.
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

package org.springframework.integration.monitor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.ClassUtils;

/**
 * @author Tareq Abedrabbo
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0.4
 */
public class MessageMetricsAdviceTests {

	private ConfigurableListableBeanFactory beanFactory;

	private IntegrationMBeanExporter mBeanExporter;

	private MessageHandler handler;

	private MessageChannel channel;

	@Before
	public void setUp() throws Exception {
		GenericApplicationContext applicationContext = TestUtils.createTestApplicationContext();
		this.beanFactory = applicationContext.getBeanFactory();
		this.channel = new NullChannel();
		this.mBeanExporter = new IntegrationMBeanExporter();
		this.mBeanExporter.setApplicationContext(applicationContext);
		this.mBeanExporter.setBeanFactory(this.beanFactory);
		this.mBeanExporter.setBeanClassLoader(ClassUtils.getDefaultClassLoader());
		this.mBeanExporter.afterPropertiesSet();
		this.handler = new DummyHandler();
		applicationContext.refresh();
	}

	@Test
	public void exportAdvisedHandler() throws Exception {

		DummyInterceptor interceptor = new DummyInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("handleMessage");

		ProxyFactory factory = new ProxyFactory(this.handler);
		factory.addAdvisor(advisor);
		MessageHandler advised = (MessageHandler) factory.getProxy();

		this.beanFactory.registerSingleton("test", advised);
		this.beanFactory.initializeBean(advised, "test");

		mBeanExporter.afterSingletonsInstantiated();
		MessageHandler exported = this.beanFactory.getBean("test", MessageHandler.class);
		exported.handleMessage(MessageBuilder.withPayload("test").build());
	}

	@Test
	public void exportAdvisedChannel() throws Exception {

		DummyInterceptor interceptor = new DummyInterceptor();
		NameMatchMethodPointcutAdvisor advisor = new NameMatchMethodPointcutAdvisor(interceptor);
		advisor.addMethodName("send");

		ProxyFactory factory = new ProxyFactory(channel);
		factory.addAdvisor(advisor);
		MessageChannel advised = (MessageChannel) factory.getProxy();

		this.beanFactory.registerSingleton("test", advised);
		this.beanFactory.initializeBean(advised, "test");

		mBeanExporter.afterSingletonsInstantiated();
		MessageChannel exported = this.beanFactory.getBean("test", MessageChannel.class);
		exported.send(MessageBuilder.withPayload("test").build());
	}

	private static class DummyHandler implements MessageHandler {

		@SuppressWarnings("unused")
		boolean invoked = false;

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			invoked = true;
		}

	}

	private static class DummyInterceptor implements MethodInterceptor {

		boolean invoked = false;

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

}
