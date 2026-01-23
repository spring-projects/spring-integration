/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.hazelcast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.BridgeFrom;
import org.springframework.integration.annotation.BridgeTo;
import org.springframework.integration.annotation.IdempotentReceiver;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class IdempotentReceiverIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private MetadataStore store;

	@Autowired
	private IdempotentReceiverInterceptor idempotentReceiverInterceptor;

	@Autowired
	private AtomicInteger adviceCalled;

	@Autowired
	private MessageChannel annotatedMethodChannel;

	@Autowired
	private FooService fooService;

	@Autowired
	private MessageChannel annotatedBeanMessageHandlerChannel;

	@Autowired
	private MessageChannel annotatedBeanMessageHandlerChannel2;

	@Autowired
	private MessageChannel bridgeChannel;

	@Autowired
	private MessageChannel toBridgeChannel;

	@Autowired
	private PollableChannel bridgePollableChannel;

	@Autowired
	private AtomicBoolean txSupplied;

	@Test
	@SuppressWarnings("unchecked")
	public void testIdempotentReceiver() {
		this.idempotentReceiverInterceptor.setThrowExceptionOnRejection(true);
		TestUtils.<Map<?, ?>>getPropertyValue(this.store, "metadata").clear();
		Message<String> message = new GenericMessage<>("foo");
		this.input.send(message);
		Message<?> receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(this.adviceCalled.get()).isEqualTo(1);
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(this.store, "metadata")).hasSize(1);
		String foo = this.store.get("foo");
		assertThat(foo).isEqualTo("FOO");

		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> this.input.send(message));

		this.idempotentReceiverInterceptor.setThrowExceptionOnRejection(false);
		this.input.send(message);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(this.adviceCalled.get()).isEqualTo(2);
		assertThat(receive.getHeaders()).containsEntry(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true);
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(store, "metadata")).hasSize(1);

		assertThat(this.txSupplied.get()).isTrue();
	}

	@Test
	public void testIdempotentReceiverOnMethod() {
		TestUtils.<Map<?, ?>>getPropertyValue(this.store, "metadata").clear();
		Message<String> message = new GenericMessage<>("foo");
		this.annotatedMethodChannel.send(message);
		this.annotatedMethodChannel.send(message);

		assertThat(this.fooService.messages.size()).isEqualTo(2);
		assertThat(this.fooService.messages.get(1)
				.getHeaders()).containsEntry(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true);
	}

	@Test
	public void testIdempotentReceiverOnBeanMessageHandler() {
		PollableChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("bar").setReplyChannel(replyChannel).build();
		this.annotatedBeanMessageHandlerChannel.send(message);

		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).doesNotContainKey(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE);

		this.annotatedBeanMessageHandlerChannel.send(message);
		receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).containsEntry(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true);

		this.annotatedBeanMessageHandlerChannel2.send(new GenericMessage<>("baz"));

		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> this.annotatedBeanMessageHandlerChannel2.send(new GenericMessage<>("baz")))
				.withMessageContaining("duplicate message has been received");
	}

	@Test
	public void testIdempotentReceiverOnBridgeTo() {
		PollableChannel replyChannel = new QueueChannel();
		Message<String> message = MessageBuilder.withPayload("bridgeTo").setReplyChannel(replyChannel).build();
		this.bridgeChannel.send(message);

		Message<?> receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).doesNotContainKey(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE);

		this.bridgeChannel.send(message);
		receive = replyChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).containsEntry(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true);
	}

	@Test
	public void testIdempotentReceiverOnBridgeFrom() {
		Message<String> message = MessageBuilder.withPayload("bridgeFrom").build();
		this.toBridgeChannel.send(message);

		Message<?> receive = this.bridgePollableChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).doesNotContainKey(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE);

		this.toBridgeChannel.send(message);
		receive = this.bridgePollableChannel.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(receive.getHeaders()).containsEntry(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, true);
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationMBeanExport(server = "mBeanServer")
	public static class ContextConfiguration {

		@Bean
		public static MBeanServerFactoryBean mBeanServer() {
			return new MBeanServerFactoryBean();
		}

		@Bean(destroyMethod = "shutdown")
		public HazelcastInstance hazelcastInstance() {
			return Hazelcast.newHazelcastInstance();
		}

		@Bean
		public ConcurrentMetadataStore store() {
			return new SimpleMetadataStore(
					hazelcastInstance()
							.getMap("idempotentReceiverMetadataStore"));
		}

		@Bean
		public IdempotentReceiverInterceptor idempotentReceiverInterceptor() {
			return new IdempotentReceiverInterceptor(
					new MetadataStoreSelector(
							message -> message.getPayload().toString(),
							message -> message.getPayload().toString().toUpperCase(), store()));
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return spy(new PseudoTransactionManager());
		}

		@Bean
		public TransactionInterceptor transactionInterceptor() {
			return new TransactionInterceptorBuilder(true)
					.build();
		}

		@Bean
		public MessageChannel input() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel output() {
			return new QueueChannel();
		}

		@Bean
		public AtomicBoolean txSupplied() {
			return new AtomicBoolean();
		}

		@Bean
		@GlobalChannelInterceptor(patterns = "output")
		public ChannelInterceptor txSuppliedChannelInterceptor(final AtomicBoolean txSupplied) {
			return new ChannelInterceptor() {

				@Override
				public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
					txSupplied.set(TransactionSynchronizationManager.isActualTransactionActive());
				}

			};
		}

		@Bean
		@org.springframework.integration.annotation.Transformer(inputChannel = "input",
				outputChannel = "output",
				adviceChain = {"fooAdvice",
						"idempotentReceiverInterceptor",
						"transactionInterceptor"})
		public Transformer transformer() {
			return message -> message;
		}

		@Bean
		public AtomicInteger adviceCalled() {
			return new AtomicInteger();
		}

		@Bean
		public Advice fooAdvice(@SuppressWarnings("unused") final AtomicInteger adviceCalled) {
			return new AbstractRequestHandlerAdvice() {

				@Override
				protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
					adviceCalled.incrementAndGet();
					return callback.execute();
				}

			};
		}

		@Bean
		public MessageChannel annotatedMethodChannel() {
			return new DirectChannel();
		}

		@Bean
		public FooService fooService() {
			return new FooService();
		}

		@Bean
		@BridgeTo
		@IdempotentReceiver("idempotentReceiverInterceptor")
		public MessageChannel bridgeChannel() {
			return new DirectChannel();
		}

		@Bean
		@BridgeFrom("toBridgeChannel")
		@IdempotentReceiver("idempotentReceiverInterceptor")
		public PollableChannel bridgePollableChannel() {
			return new QueueChannel();
		}

		@Bean
		@ServiceActivator(inputChannel = "annotatedBeanMessageHandlerChannel")
		@IdempotentReceiver("idempotentReceiverInterceptor")
		public MessageHandler messageHandler() {
			return new ServiceActivatingHandler((MessageProcessor<Object>) message -> message);
		}

		@Bean
		@ServiceActivator(inputChannel = "annotatedBeanMessageHandlerChannel2")
		@IdempotentReceiver("idempotentReceiverInterceptor")
		public MessageHandler messageHandler2() {
			return message -> {
				if (message.getHeaders().containsKey(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE)) {
					throw new MessageHandlingException(message, "duplicate message has been received");
				}
			};
		}

	}

	@Component
	private static class FooService {

		private final List<Message<?>> messages = new ArrayList<Message<?>>();

		@ServiceActivator(inputChannel = "annotatedMethodChannel")
		@IdempotentReceiver("idempotentReceiverInterceptor")
		public void handle(Message<?> message) {
			this.messages.add(message);
		}

	}

}
