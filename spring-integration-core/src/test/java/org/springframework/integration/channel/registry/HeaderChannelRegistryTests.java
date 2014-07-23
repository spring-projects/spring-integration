/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.integration.channel.registry;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.DefaultHeaderChannelRegistry;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class HeaderChannelRegistryTests {

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
		Message<?> reply = template.sendAndReceive(new GenericMessage<String>("foo"));
		assertNotNull(reply);
		assertEquals("echo:foo", reply.getPayload());
		String stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(registry, "channels", Map.class)
					.get(stringReplyChannel), "expireAt", Long.class) - System.currentTimeMillis(),
						lessThan(61000L));
	}

	@Test
	public void testReplaceTtl() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.inputTtl);
		Message<?> reply = template.sendAndReceive(new GenericMessage<String>("ttl"));
		assertNotNull(reply);
		assertEquals("echo:ttl", reply.getPayload());
		String stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(registry, "channels", Map.class)
					.get(stringReplyChannel), "expireAt", Long.class) - System.currentTimeMillis(),
						greaterThan(100000L));
	}

	@Test
	public void testReplaceCustomTtl() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.inputCustomTtl);
		Message<String> requestMessage = MessageBuilder.withPayload("ttl")
				.setHeader("channelTTL", 180000)
				.build();
		Message<?> reply = template.sendAndReceive(requestMessage);
		assertNotNull(reply);
		assertEquals("echo:ttl", reply.getPayload());
		String stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(registry, "channels", Map.class)
					.get(stringReplyChannel), "expireAt", Long.class) - System.currentTimeMillis(),
						allOf(greaterThan(160000L), lessThan(181000L)));
		// Now for Elvis...
		reply = template.sendAndReceive(new GenericMessage<String>("ttl"));
		assertNotNull(reply);
		assertEquals("echo:ttl", reply.getPayload());
		stringReplyChannel = reply.getHeaders().get("stringReplyChannel", String.class);
		assertThat(TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(registry, "channels", Map.class)
					.get(stringReplyChannel), "expireAt", Long.class) - System.currentTimeMillis(),
						greaterThan(220000L));
	}

	@Test
	public void testReplaceGatewayWithNoReplyChannel() {
		String reply = this.gatewayNoReplyChannel.exchange("foo");
		assertNotNull(reply);
		assertEquals("echo:foo", reply);
	}

	@Test
	public void testReplaceGatewayWithExplicitReplyChannel() {
		String reply = this.gatewayExplicitReplyChannel.exchange("foo");
		assertNotNull(reply);
		assertEquals("echo:foo", reply);
	}

	/**
	 * MessagingTemplate sets the errorChannel to the replyChannel so it gets any async
	 * exceptions via the default {@link MessagePublishingErrorHandler}.
	 */
	@Test
	public void testReplaceError() {
		MessagingTemplate template = new MessagingTemplate();
		template.setDefaultDestination(this.inputPolled);
		Message<?> reply = template.sendAndReceive(new GenericMessage<String>("bar"));
		assertNotNull(reply);
		assertTrue(reply instanceof ErrorMessage);
	}

	@Test
	public void testAlreadyAString() {
		Message<String> requestMessage = MessageBuilder.withPayload("foo")
				.setReplyChannelName("alreadyAString")
				.setErrorChannelName("alreadyAnotherString")
				.build();
		this.input.send(requestMessage);
		Message<?> reply = alreadyAString.receive(0);
		assertNotNull(reply);
		assertEquals("echo:foo", reply.getPayload());
	}

	@Test
	public void testNull() {
		Message<String> requestMessage = MessageBuilder.withPayload("foo")
				.build();
		try {
			this.input.send(requestMessage);
			fail("expected exception");
		}
		catch (Exception e) {
			assertThat(e.getMessage(), Matchers.containsString("no output-channel or replyChannel"));
		}
	}

	@Test
	public void testExpire() throws Exception {
		DefaultHeaderChannelRegistry registry = new DefaultHeaderChannelRegistry(50);
		registry.setTaskScheduler(this.taskScheduler);
		registry.start();
		String id = (String) registry.channelToChannelName(new DirectChannel());
		int n = 0;
		while (n++ < 100 && registry.channelNameToChannel(id) != null) {
			Thread.sleep(100);
		}
		assertNull(registry.channelNameToChannel(id));
		registry.stop();
	}

	@Test
	public void testBFCRWithRegistry() {
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver();
		BeanFactory beanFactory = mock(BeanFactory.class);
		when(beanFactory.getBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME,
						HeaderChannelRegistry.class))
			.thenReturn(mock(HeaderChannelRegistry.class));
		doAnswer(new Answer<Object>(){

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				throw new NoSuchBeanDefinitionException("bar");
			}
		}).when(beanFactory).getBean("foo", MessageChannel.class);
		resolver.setBeanFactory(beanFactory);
		try {
			resolver.resolveDestination("foo");
			fail("Expected exception");
		}
		catch (DestinationResolutionException e){
			assertThat(e.getMessage(),
				Matchers.equalTo("failed to look up MessageChannel with name 'foo' in the BeanFactory."));
		}
	}

	@Test
	public void testBFCRNoRegistry() {
		BeanFactoryChannelResolver resolver = new BeanFactoryChannelResolver();
		BeanFactory beanFactory = mock(BeanFactory.class);
		doAnswer(new Answer<Object>(){

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				throw new NoSuchBeanDefinitionException("bar");
			}
		}).when(beanFactory).getBean("foo", MessageChannel.class);
		resolver.setBeanFactory(beanFactory);
		try {
			resolver.resolveDestination("foo");
			fail("Expected exception");
		}
		catch (DestinationResolutionException e){
			assertThat(e.getMessage(),
				Matchers.equalTo("failed to look up MessageChannel with name 'foo' in the BeanFactory (and there is no HeaderChannelRegistry present)."));
		}
	}

	@Test
	public void testRemoveOnGet() {
		DefaultHeaderChannelRegistry registry = new DefaultHeaderChannelRegistry();
		MessageChannel channel = new DirectChannel();
		String foo = (String) registry.channelToChannelName(channel);
		Map<?, ?> map = TestUtils.getPropertyValue(registry, "channels", Map.class);
		assertEquals(1, map.size());
		assertSame(channel, registry.channelNameToChannel(foo));
		assertEquals(1, map.size());
		registry.setRemoveOnGet(true);
		assertSame(channel, registry.channelNameToChannel(foo));
		assertEquals(0, map.size());
	}


	public static class Foo extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			assertThat(requestMessage.getHeaders().getReplyChannel(),
					Matchers.anyOf(instanceOf(String.class), Matchers.nullValue()));
			assertThat(requestMessage.getHeaders().getErrorChannel(),
					Matchers.anyOf(instanceOf(String.class), Matchers.nullValue()));
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
