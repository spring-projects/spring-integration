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

package org.springframework.integration.handler.advice;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
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
public class IdempotentReceiverTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel input2;

	@Autowired
	private PollableChannel output;

	@Autowired
	private MetadataStore store;

	@Autowired
	private MetadataStore store2;

	@Autowired
	private IdempotentReceiverInterceptor idempotentReceiverInterceptor;

	@Autowired
	private FooAdvice fooAdvice;

	@Test
	public void testIdempotentReceiverInterceptor() {
		ConcurrentMetadataStore store = new SimpleMetadataStore();
		ExpressionEvaluatingMessageProcessor<String> idempotentKeyStrategy =
				new ExpressionEvaluatingMessageProcessor<>(new SpelExpressionParser().parseExpression("payload"));
		BeanFactory beanFactory = Mockito.mock(BeanFactory.class);
		idempotentKeyStrategy.setBeanFactory(beanFactory);
		IdempotentReceiverInterceptor idempotentReceiverInterceptor =
				new IdempotentReceiverInterceptor(new MetadataStoreSelector(idempotentKeyStrategy, store));
		idempotentReceiverInterceptor.setThrowExceptionOnRejection(true);

		AtomicReference<Message<?>> handled = new AtomicReference<>();

		MessageHandler idempotentReceiver = handled::set;

		ProxyFactory proxyFactory = new ProxyFactory(idempotentReceiver);
		proxyFactory.addAdvice(idempotentReceiverInterceptor);
		idempotentReceiver = (MessageHandler) proxyFactory.getProxy();

		idempotentReceiver.handleMessage(new GenericMessage<>("foo"));
		assertEquals(1, TestUtils.getPropertyValue(store, "metadata", Map.class).size());
		assertNotNull(store.get("foo"));

		try {
			idempotentReceiver.handleMessage(new GenericMessage<>("foo"));
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(MessageRejectedException.class));
		}

		idempotentReceiverInterceptor.setThrowExceptionOnRejection(false);
		idempotentReceiver.handleMessage(new GenericMessage<>("foo"));
		assertTrue(handled.get().getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE,
				Boolean.class));
		assertEquals(1, TestUtils.getPropertyValue(store, "metadata", Map.class).size());
	}

	@Test
	public void testIdempotentReceiver() {
		Message<String> message = new GenericMessage<>("foo");
		this.input.send(message);
		Message<?> receive = this.output.receive(10000);
		assertNotNull(receive);
		assertEquals(1, this.fooAdvice.adviceCalled);
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
		assertEquals(2, this.fooAdvice.adviceCalled);
		assertTrue(receive.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class));
		assertEquals(1, TestUtils.getPropertyValue(this.store, "metadata", Map.class).size());

		message = new GenericMessage<>("bar");
		for (int i = 0; i < 2; i++) {
			this.input2.send(message);
			receive = this.output.receive(10000);
			assertNotNull(receive);
		}

		assertTrue(receive.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class));
		assertEquals(2, TestUtils.getPropertyValue(this.store, "metadata", Map.class).size());
		assertNotNull(this.store.get("bar"));
		assertEquals(1, TestUtils.getPropertyValue(this.store2, "metadata", Map.class).size());
		assertNotNull(this.store2.get("BAR"));
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		private int adviceCalled;

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}

}
