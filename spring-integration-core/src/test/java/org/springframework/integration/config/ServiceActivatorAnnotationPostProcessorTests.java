/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.TestApplicationContext;

/**
 * @author Mark Fisher
 */
public class ServiceActivatorAnnotationPostProcessorTests {

	@Test
	public void testAnnotatedMethod() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		TestApplicationContext context = TestUtils.createTestApplicationContext();
		RootBeanDefinition postProcessorDef = new RootBeanDefinition(MessagingAnnotationPostProcessor.class);
		context.registerBeanDefinition("postProcessor", postProcessorDef);
		context.registerBeanDefinition("testChannel", new RootBeanDefinition(DirectChannel.class));
		RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleServiceActivatorAnnotationTestBean.class);
		beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(latch);
		context.registerBeanDefinition("testBean", beanDefinition);
		context.refresh();
		SimpleServiceActivatorAnnotationTestBean testBean = (SimpleServiceActivatorAnnotationTestBean) context.getBean("testBean");
		assertEquals(1, latch.getCount());
		assertNull(testBean.getMessageText());
		MessageChannel testChannel = (MessageChannel) context.getBean("testChannel");
		testChannel.send(new GenericMessage<String>("test-123"));
		latch.await(1000, TimeUnit.MILLISECONDS);
		assertEquals(0, latch.getCount());
		assertEquals("test-123", testBean.getMessageText());
		context.stop();
	}


	public static class AbstractServiceActivatorAnnotationTestBean {

		protected String messageText;

		private CountDownLatch latch;

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

		@ServiceActivator(inputChannel="testChannel")
		public void testMethod(String messageText) {
			this.messageText = messageText;
			this.countDown();
		}
	}

}
