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

package org.springframework.integration.handler.advice;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

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
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class IdempotentReceiverTests implements TestApplicationContextAware {

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
	private TestAdvice testAdvice;

	@Test
	public void testIdempotentReceiverInterceptor() {
		ConcurrentMetadataStore store = new SimpleMetadataStore();
		ExpressionEvaluatingMessageProcessor<String> idempotentKeyStrategy =
				new ExpressionEvaluatingMessageProcessor<>(new SpelExpressionParser().parseExpression("payload"));
		BeanFactory beanFactory = TEST_INTEGRATION_CONTEXT;
		idempotentKeyStrategy.setBeanFactory(beanFactory);
		IdempotentReceiverInterceptor idempotentReceiverInterceptor =
				new IdempotentReceiverInterceptor(new MetadataStoreSelector(idempotentKeyStrategy, store));
		idempotentReceiverInterceptor.setThrowExceptionOnRejection(true);

		AtomicReference<Message<?>> handled = new AtomicReference<>();

		MessageHandler idempotentReceiver = handled::set;

		ProxyFactory proxyFactory = new ProxyFactory(idempotentReceiver);
		proxyFactory.addAdvice(idempotentReceiverInterceptor);
		idempotentReceiver = (MessageHandler) proxyFactory.getProxy();

		idempotentReceiver.handleMessage(new GenericMessage<>("testData"));
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(store, "metadata").size()).isEqualTo(1);
		assertThat(store.get("testData")).isNotNull();

		try {
			idempotentReceiver.handleMessage(new GenericMessage<>("testData"));
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(MessageRejectedException.class);
		}

		idempotentReceiverInterceptor.setThrowExceptionOnRejection(false);
		idempotentReceiver.handleMessage(new GenericMessage<>("testData"));
		assertThat(handled.get().getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE,
				Boolean.class)).isTrue();
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(store, "metadata").size()).isEqualTo(1);
	}

	@Test
	public void testIdempotentReceiver() {
		Message<String> message = new GenericMessage<>("testData");
		this.input.send(message);
		Message<?> receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(this.testAdvice.adviceCalled).isEqualTo(1);
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(this.store, "metadata").size()).isEqualTo(1);
		assertThat(this.store.get("testData")).isNotNull();

		try {
			this.input.send(message);
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(MessageRejectedException.class);
		}
		this.idempotentReceiverInterceptor.setThrowExceptionOnRejection(false);
		this.input.send(message);
		receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(this.testAdvice.adviceCalled).isEqualTo(2);
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class))
				.isTrue();
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(this.store, "metadata").size()).isEqualTo(1);

		message = new GenericMessage<>("bar");
		for (int i = 0; i < 2; i++) {
			this.input2.send(message);
			receive = this.output.receive(10000);
			assertThat(receive).isNotNull();
		}

		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class))
				.isTrue();
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(this.store, "metadata").size()).isEqualTo(2);
		assertThat(this.store.get("bar")).isNotNull();
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(this.store2, "metadata").size()).isEqualTo(1);
		assertThat(this.store2.get("BAR")).isNotNull();
	}

	public static class TestAdvice extends AbstractRequestHandlerAdvice {

		private int adviceCalled;

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
