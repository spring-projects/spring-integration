/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ServiceActivatorAnnotationPostProcessorTests {

	@Test
	public void testAnnotatedMethod() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		try (TestApplicationContext context = TestUtils.createTestApplicationContext()) {
			new IntegrationRegistrar().registerBeanDefinitions(mock(), context.getDefaultListableBeanFactory());
			context.registerBeanDefinition("testChannel", new RootBeanDefinition(DirectChannel.class));
			RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleServiceActivatorAnnotationTestBean.class);
			beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(latch);
			context.registerBeanDefinition("testBean", beanDefinition);
			context.refresh();
			SimpleServiceActivatorAnnotationTestBean testBean =
					context.getBean("testBean", SimpleServiceActivatorAnnotationTestBean.class);
			assertThat(latch.getCount()).isEqualTo(1);
			assertThat(testBean.getMessageText()).isNull();
			MessageChannel testChannel = (MessageChannel) context.getBean("testChannel");
			testChannel.send(new GenericMessage<>("test-123"));
			assertThat(latch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
			assertThat(testBean.getMessageText()).isEqualTo("test-123");
		}
	}

	public static class AbstractServiceActivatorAnnotationTestBean {

		private final CountDownLatch latch;

		protected String messageText;

		public AbstractServiceActivatorAnnotationTestBean(CountDownLatch latch) {
			this.latch = latch;
		}

		protected void countDown() {
			this.latch.countDown();
		}

		public String getMessageText() {
			return this.messageText;
		}

	}

	@MessageEndpoint
	public static class SimpleServiceActivatorAnnotationTestBean extends AbstractServiceActivatorAnnotationTestBean {

		public SimpleServiceActivatorAnnotationTestBean(CountDownLatch latch) {
			super(latch);
		}

		@ServiceActivator(inputChannel = "testChannel")
		public void testMethod(String messageText) {
			this.messageText = messageText;
			this.countDown();
		}

	}

}
