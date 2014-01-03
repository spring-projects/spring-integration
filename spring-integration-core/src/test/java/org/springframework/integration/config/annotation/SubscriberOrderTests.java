/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config.annotation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class SubscriberOrderTests {

	@Test
	public void directChannelAndFailoverDispatcherWithSingleCallPerMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("postProcessor", new RootBeanDefinition(MessagingAnnotationPostProcessor.class));
		RootBeanDefinition channelDefinition = new RootBeanDefinition(DirectChannel.class);
		context.registerBeanDefinition("input", channelDefinition);
		RootBeanDefinition testBeanDefinition = new RootBeanDefinition(TestBean.class);
		testBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1);
		context.registerBeanDefinition("testBean", testBeanDefinition);
		context.refresh();
		TestBean testBean = (TestBean) context.getBean("testBean");
		MessageChannel channel = (MessageChannel) context.getBean("input");
		channel.send(new GenericMessage<String>("test-1"));
		channel.send(new GenericMessage<String>("test-2"));
		channel.send(new GenericMessage<String>("test-3"));
		channel.send(new GenericMessage<String>("test-4"));
		channel.send(new GenericMessage<String>("test-5"));
		List<Integer> calls = testBean.calls;
		assertEquals(5, calls.size());
		assertEquals(1, calls.get(0).intValue());
		assertEquals(2, calls.get(1).intValue());
		assertEquals(3, calls.get(2).intValue());
		assertEquals(4, calls.get(3).intValue());
		assertEquals(5, calls.get(4).intValue());
	}

	@Test
	public void directChannelAndFailoverDispatcherWithMultipleCallsPerMethod() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("postProcessor", new RootBeanDefinition(MessagingAnnotationPostProcessor.class));
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.rootBeanDefinition(DirectChannel.class);
		channelBuilder.addConstructorArgValue(null);
		RootBeanDefinition channelDefinition = (RootBeanDefinition) channelBuilder.getBeanDefinition();
		context.registerBeanDefinition("input", channelDefinition);
		RootBeanDefinition testBeanDefinition = new RootBeanDefinition(TestBean.class);
		testBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(2);
		context.registerBeanDefinition("testBean", testBeanDefinition);
		context.refresh();
		TestBean testBean = (TestBean) context.getBean("testBean");
		MessageChannel channel = (MessageChannel) context.getBean("input");
		channel.send(new GenericMessage<String>("test-1"));
		channel.send(new GenericMessage<String>("test-2"));
		channel.send(new GenericMessage<String>("test-3"));
		channel.send(new GenericMessage<String>("test-4"));
		channel.send(new GenericMessage<String>("test-5"));
		channel.send(new GenericMessage<String>("test-6"));
		channel.send(new GenericMessage<String>("test-7"));
		channel.send(new GenericMessage<String>("test-8"));
		channel.send(new GenericMessage<String>("test-9"));
		channel.send(new GenericMessage<String>("test-10"));
		assertEquals(10, testBean.calls.size());
		assertEquals(1, testBean.calls.get(0).intValue());
		assertEquals(1, testBean.calls.get(1).intValue());
		assertEquals(2, testBean.calls.get(2).intValue());
		assertEquals(2, testBean.calls.get(3).intValue());
		assertEquals(3, testBean.calls.get(4).intValue());
		assertEquals(3, testBean.calls.get(5).intValue());
		assertEquals(4, testBean.calls.get(6).intValue());
		assertEquals(4, testBean.calls.get(7).intValue());
		assertEquals(5, testBean.calls.get(8).intValue());
		assertEquals(5, testBean.calls.get(9).intValue());
		testBean.reset();
		channel.send(new GenericMessage<String>("test-11"));
		assertEquals(1, testBean.calls.size());
		assertEquals(1, testBean.calls.get(0).intValue());
	}

	@Test
	public void directChannelAndRoundRobinDispatcher() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBeanDefinition("postProcessor", new RootBeanDefinition(MessagingAnnotationPostProcessor.class));
		RootBeanDefinition channelDefinition = new RootBeanDefinition(DirectChannel.class);
		channelDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RoundRobinLoadBalancingStrategy());
		context.registerBeanDefinition("input", channelDefinition);
		RootBeanDefinition testBeanDefinition = new RootBeanDefinition(TestBean.class);
		testBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1000);
		context.registerBeanDefinition("testBean", testBeanDefinition);
		context.refresh();
		TestBean testBean = (TestBean) context.getBean("testBean");
		MessageChannel channel = (MessageChannel) context.getBean("input");
		channel.send(new GenericMessage<String>("test-1"));
		channel.send(new GenericMessage<String>("test-2"));
		channel.send(new GenericMessage<String>("test-3"));
		channel.send(new GenericMessage<String>("test-4"));
		channel.send(new GenericMessage<String>("test-5"));
		List<Integer> calls = testBean.calls;
		assertEquals(5, calls.size());
		assertEquals(1, calls.get(0).intValue());
		assertEquals(2, calls.get(1).intValue());
		assertEquals(3, calls.get(2).intValue());
		assertEquals(4, calls.get(3).intValue());
		assertEquals(5, calls.get(4).intValue());
	}


	static abstract class AbstractTestBean {

		@Order(4)
		abstract void fourth(Message<?> message);

		@Order(2)
		abstract void second(Message<?> message);

	}


	@MessageEndpoint
	static class TestBean extends AbstractTestBean {

		private final int maxCallsPerMethod;

		private volatile List<Integer> calls = new ArrayList<Integer>();


		TestBean(int maxCallsPerMethod) {
			this.maxCallsPerMethod = maxCallsPerMethod;
		}


		void reset() {
			this.calls = new ArrayList<Integer>();
		}

		@Order(3)
		@ServiceActivator(inputChannel="input")
		public void third(Message<?> message) {
			this.handle(3, message);
		}

		@Override
		@ServiceActivator(inputChannel="input")
		public void second(Message<?> message) {
			this.handle(2, message);
		}

		@Order(1)
		@ServiceActivator(inputChannel="input")
		public void first(Message<?> message) {
			this.handle(1, message);
		}

		@Order(5)
		@ServiceActivator(inputChannel="input")
		public void fifth(Message<?> message) {
			this.handle(5, message);
		}

		@Override
		@ServiceActivator(inputChannel="input")
		public void fourth(Message<?> message) {
			this.handle(4, message);
		}

		private void handle(int methodNumber, Message<?> message) {
			int count = 0;
			for (int callNumber : this.calls) {
				if (callNumber == methodNumber) {
					count++;
					if (count >= this.maxCallsPerMethod) {
						throw new MessageRejectedException(message, null);
					}
				}
			}
			this.calls.add(methodNumber);
		}
	}

}
