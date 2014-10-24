/*
 * Copyright 2014 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.annotation.IdempotentReceiver;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataKeyStrategy;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.Transformer;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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

	@Test
	public void testIdempotentReceiver() {
		Message<String> message = new GenericMessage<String>("foo");
		this.input.send(message);
		Message<?> receive = this.output.receive(10000);
		assertNotNull(receive);
		assertEquals(1, this.adviceCalled.get());
		assertEquals(1, TestUtils.getPropertyValue(this.store, "metadata", Map.class).size());
		assertNotNull(this.store.get("foo"));

		try {
			this.input.send(message);
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageRejectedException.class));
		}
		this.idempotentReceiverInterceptor.setThrowExceptionOnRejection(false);
		this.input.send(message);
		receive = this.output.receive(10000);
		assertNotNull(receive);
		assertEquals(2, this.adviceCalled.get());
		assertTrue(receive.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class));
		assertEquals(1, TestUtils.getPropertyValue(store, "metadata", Map.class).size());
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationMBeanExport(server = "mBeanServer")
	public static class ContextConfiguration {

		@Bean
		public static MBeanServerFactoryBean mBeanServer() {
			return new MBeanServerFactoryBean();
		}

		@Bean
		public ConcurrentMetadataStore store() {
			return new SimpleMetadataStore();
		}

		@Bean
		public IdempotentReceiverInterceptor idempotentReceiverInterceptor() {
			IdempotentReceiverInterceptor idempotentReceiverInterceptor =
					new IdempotentReceiverInterceptor(new MetadataStoreSelector(new MetadataKeyStrategy() {

						@Override
						public String getKey(Message<?> message) {
							return message.getPayload().toString();
						}

					}, store()));
			idempotentReceiverInterceptor.setThrowExceptionOnRejection(true);
			return idempotentReceiverInterceptor;
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
		@org.springframework.integration.annotation.Transformer(inputChannel = "input",
				outputChannel = "output", adviceChain = "fooAdvice")
		@IdempotentReceiver("idempotentReceiverInterceptor")
		public Transformer transformer() {
			return new Transformer() {

				@Override
				public Message<?> transform(Message<?> message) {
					return message;
				}

			};
		}

		@Bean
		public AtomicInteger adviceCalled() {
			return new AtomicInteger();
		}

		@Bean
		public Advice fooAdvice(final AtomicInteger adviceCalled) {
			return new AbstractRequestHandlerAdvice() {

				@Override
				protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message)
						throws Exception {
					adviceCalled.incrementAndGet();
					return callback.execute();
				}

			};
		}

	}

}
