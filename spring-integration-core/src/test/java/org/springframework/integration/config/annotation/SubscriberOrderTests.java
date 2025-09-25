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

package org.springframework.integration.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class SubscriberOrderTests {

	@Test
	public void directChannelAndFailoverDispatcherWithSingleCallPerMethod() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			new IntegrationRegistrar().registerBeanDefinitions(mock(), context.getDefaultListableBeanFactory());
			RootBeanDefinition channelDefinition = new RootBeanDefinition(DirectChannel.class);
			context.registerBeanDefinition("input", channelDefinition);
			RootBeanDefinition testBeanDefinition = new RootBeanDefinition(TestBean.class);
			testBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1);
			context.registerBeanDefinition("testBean", testBeanDefinition);
			context.refresh();
			TestBean testBean = (TestBean) context.getBean("testBean");
			MessageChannel channel = (MessageChannel) context.getBean("input");
			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<>("test-" + (i + 1)));
			}

			List<Integer> calls = testBean.calls;
			assertThat(calls).hasSize(5)
					.containsExactlyInAnyOrder(1, 2, 3, 4, 5);
		}
	}

	@Test
	public void directChannelAndFailoverDispatcherWithMultipleCallsPerMethod() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			new IntegrationRegistrar().registerBeanDefinitions(mock(), context.getDefaultListableBeanFactory());
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
			for (int i = 0; i < 10; i++) {
				channel.send(new GenericMessage<>("test-" + (i + 1)));
			}

			assertThat(testBean.calls).hasSize(10)
					.containsExactly(1, 1, 2, 2, 3, 3, 4, 4, 5, 5);
			testBean.reset();
			channel.send(new GenericMessage<>("test-11"));
			assertThat(testBean.calls.size()).isEqualTo(1);
			assertThat(testBean.calls.get(0).intValue()).isEqualTo(1);
		}
	}

	@Test
	public void directChannelAndRoundRobinDispatcher() {
		try (GenericApplicationContext context = new GenericApplicationContext()) {
			new IntegrationRegistrar().registerBeanDefinitions(mock(), context.getDefaultListableBeanFactory());
			RootBeanDefinition channelDefinition = new RootBeanDefinition(DirectChannel.class);
			channelDefinition.getConstructorArgumentValues()
					.addGenericArgumentValue(new RoundRobinLoadBalancingStrategy());
			context.registerBeanDefinition("input", channelDefinition);
			RootBeanDefinition testBeanDefinition = new RootBeanDefinition(TestBean.class);
			testBeanDefinition.getConstructorArgumentValues().addGenericArgumentValue(1000);
			context.registerBeanDefinition("testBean", testBeanDefinition);
			context.refresh();
			TestBean testBean = (TestBean) context.getBean("testBean");
			MessageChannel channel = (MessageChannel) context.getBean("input");

			for (int i = 0; i < 5; i++) {
				channel.send(new GenericMessage<>("test-" + (i + 1)));
			}

			List<Integer> calls = testBean.calls;
			assertThat(calls).hasSize(5)
					.containsExactly(1, 2, 3, 4, 5);
		}
	}

	abstract static class AbstractTestBean {

		@Order(4)
		abstract void fourth(Message<?> message);

		@Order(2)
		abstract void second(Message<?> message);

	}

	@MessageEndpoint
	static class TestBean extends AbstractTestBean {

		private final int maxCallsPerMethod;

		private final List<Integer> calls = new ArrayList<>();

		TestBean(int maxCallsPerMethod) {
			this.maxCallsPerMethod = maxCallsPerMethod;
		}

		void reset() {
			this.calls.clear();
		}

		@Order(3)
		@ServiceActivator(inputChannel = "input")
		public void third(Message<?> message) {
			this.handle(3, message);
		}

		@Override
		@ServiceActivator(inputChannel = "input")
		public void second(Message<?> message) {
			this.handle(2, message);
		}

		@Order(1)
		@ServiceActivator(inputChannel = "input")
		public void first(Message<?> message) {
			this.handle(1, message);
		}

		@Order(5)
		@ServiceActivator(inputChannel = "input")
		public void fifth(Message<?> message) {
			this.handle(5, message);
		}

		@Override
		@ServiceActivator(inputChannel = "input")
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
