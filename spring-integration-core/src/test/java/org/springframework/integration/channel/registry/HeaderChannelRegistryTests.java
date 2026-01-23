/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.channel.registry;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class HeaderChannelRegistryTests implements TestApplicationContextAware {

	@Autowired
	MessageChannel input;

	@Autowired
	MessageChannel inputTtl;

	@Autowired
	MessageChannel inputCustomTtl;

	@Autowired
	MessageChannel inputPolled;

	@Autowired
	QueueChannel alreadyAString;

	@Autowired
	TaskScheduler taskScheduler;

	@Autowired
	Gateway gatewayNoReplyChannel;

	@Autowired
	Gateway gatewayExplicitReplyChannel;

	@Autowired
	DefaultHeaderChannelRegistry registry;

	@Test
	public void testReplace() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.input);
		template.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Message<?> reply = template.sendAndReceive(new GenericMessage<>("foo"));
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("echo:foo");
		String stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.<Long>getPropertyValue(
				TestUtils.<Map<?, ?>>getPropertyValue(registry, "channels")
						.get(stringReplyChannel), "expireAt") - System.currentTimeMillis())
				.isLessThan(61000L);
	}

	@Test
	public void testReplaceTtl() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.inputTtl);
		template.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Message<?> reply = template.sendAndReceive(new GenericMessage<>("ttl"));
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("echo:ttl");
		String stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.<Long>getPropertyValue(
				TestUtils.<Map<?, ?>>getPropertyValue(registry, "channels")
						.get(stringReplyChannel), "expireAt") - System.currentTimeMillis())
				.isGreaterThan(100000L);
	}

	@Test
	public void testReplaceCustomTtl() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.inputCustomTtl);
		template.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Message<String> requestMessage = MessageBuilder.withPayload("ttl")
				.setHeader("channelTTL", 180000)
				.build();
		Message<?> reply = template.sendAndReceive(requestMessage);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("echo:ttl");
		String stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.<Long>getPropertyValue(
				TestUtils.<Map<?, ?>>getPropertyValue(registry, "channels")
						.get(stringReplyChannel), "expireAt") - System.currentTimeMillis())
				.isGreaterThan(160000L).isLessThan(181000L);
		// Now for Elvis...
		reply = template.sendAndReceive(new GenericMessage<>("ttl"));
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("echo:ttl");
		stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.<Long>getPropertyValue(
				TestUtils.<Map<?, ?>>getPropertyValue(registry, "channels")
						.get(stringReplyChannel), "expireAt") - System.currentTimeMillis())
				.isGreaterThan(220000L);
	}

	@Test
	public void testReplaceGatewayWithNoReplyChannel() {
		String reply = this.gatewayNoReplyChannel.exchange("foo");
		assertThat(reply).isNotNull();
		assertThat(reply).isEqualTo("echo:foo");
	}

	@Test
	public void testReplaceGatewayWithExplicitReplyChannel() {
		String reply = this.gatewayExplicitReplyChannel.exchange("foo");
		assertThat(reply).isNotNull();
		assertThat(reply).isEqualTo("echo:foo");
	}

	/**
	 * MessagingTemplate sets the errorChannel to the replyChannel so it gets any async
	 * exceptions via the default {@link MessagePublishingErrorHandler}.
	 */
	@Test
	public void testReplaceError() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.inputPolled);
		template.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		Message<?> reply = template.sendAndReceive(new GenericMessage<>("bar"));
		assertThat(reply).isNotNull();
		assertThat(reply instanceof ErrorMessage).isTrue();
		assertThat(((ErrorMessage) reply).getOriginalMessage()).isNotNull();
		assertThat(reply.getPayload()).isNotInstanceOf(MessagingExceptionWrapper.class);
	}

	@Test
	public void testAlreadyAString() {
		Message<String> requestMessage = MessageBuilder.withPayload("foo")
				.setReplyChannelName("alreadyAString")
				.setErrorChannelName("alreadyAnotherString")
				.build();
		this.input.send(requestMessage);
		Message<?> reply = alreadyAString.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("echo:foo");
	}

	@Test
	public void testNull() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> this.input.send(new GenericMessage<>("test")))
				.withStackTraceContaining("no output-channel or replyChannel");
	}

	@Test
	public void testExpire() throws Exception {
		DefaultHeaderChannelRegistry registry = new DefaultHeaderChannelRegistry(50);
		registry.setTaskScheduler(this.taskScheduler);
		String id = (String) registry.channelToChannelName(new DirectChannel());
		int n = 0;
		while (n++ < 100 && registry.channelNameToChannel(id) != null) {
			Thread.sleep(100);
		}
		assertThat(registry.channelNameToChannel(id)).isNull();
		registry.stop();
	}

	@Test
	public void testBFCRWithRegistry() {
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver();
		TestUtils.registerBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME,
				new DefaultHeaderChannelRegistry(), TEST_INTEGRATION_CONTEXT);
		resolver.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		assertThatExceptionOfType(DestinationResolutionException.class)
				.isThrownBy(() -> resolver.resolveDestination("foo"))
				.withMessageContaining("failed to look up MessageChannel with name 'foo' in the BeanFactory.");

		TEST_INTEGRATION_CONTEXT.removeBeanDefinition(
				IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME);
	}

	@Test
	public void testBFCRNoRegistry() {
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver();
		resolver.setBeanFactory(TEST_INTEGRATION_CONTEXT);

		assertThatExceptionOfType(DestinationResolutionException.class)
				.isThrownBy(() -> resolver.resolveDestination("foo"))
				.withMessageContaining("failed to look up MessageChannel with name 'foo' in the BeanFactory" +
						" (and there is no HeaderChannelRegistry present).");
	}

	@Test
	public void testRemoveOnGet() {
		DefaultHeaderChannelRegistry registry = new DefaultHeaderChannelRegistry();
		registry.setTaskScheduler(new SimpleAsyncTaskScheduler());
		MessageChannel channel = new DirectChannel();
		String foo = (String) registry.channelToChannelName(channel);
		Map<?, ?> map = TestUtils.getPropertyValue(registry, "channels");
		assertThat(map).hasSize(1);
		assertThat(registry.channelNameToChannel(foo)).isSameAs(channel);
		assertThat(map).hasSize(1);
		registry.setRemoveOnGet(true);
		assertThat(registry.channelNameToChannel(foo)).isSameAs(channel);
		assertThat(map).hasSize(0);
	}

	public static class Foo extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			assertThat(requestMessage.getHeaders().getReplyChannel())
					.satisfiesAnyOf(
							replyChannel -> assertThat(replyChannel).isInstanceOf(String.class),
							replyChannel -> assertThat(replyChannel).isNull());
			assertThat(requestMessage.getHeaders().getErrorChannel())
					.satisfiesAnyOf(
							errorChannel -> assertThat(errorChannel).isInstanceOf(String.class),
							errorChannel -> assertThat(errorChannel).isNull());
			if (requestMessage.getPayload().equals("bar")) {
				throw new RuntimeException("intentional");
			}
			return MessageBuilder.withPayload("echo:" + requestMessage.getPayload())
					.setHeader("stringReplyChannel", requestMessage.getHeaders().getReplyChannel())
					.build();
		}

	}

	public interface Gateway {

		String exchange(String foo);

	}

}
