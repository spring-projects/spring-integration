/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.handler.advice;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SpringJUnitConfig
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
		assertThat(TestUtils.getPropertyValue(store, "metadata", Map.class).size()).isEqualTo(1);
		assertThat(store.get("foo")).isNotNull();

		try {
			idempotentReceiver.handleMessage(new GenericMessage<>("foo"));
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(MessageRejectedException.class);
		}

		idempotentReceiverInterceptor.setThrowExceptionOnRejection(false);
		idempotentReceiver.handleMessage(new GenericMessage<>("foo"));
		assertThat(handled.get().getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE,
				Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(store, "metadata", Map.class).size()).isEqualTo(1);
	}

	@Test
	public void testIdempotentReceiver() {
		Message<String> message = new GenericMessage<>("foo");
		this.input.send(message);
		Message<?> receive = this.output.receive(10000);
		assertThat(receive).isNotNull();
		assertThat(this.fooAdvice.adviceCalled).isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(this.store, "metadata", Map.class).size()).isEqualTo(1);
		assertThat(this.store.get("foo")).isNotNull();

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
		assertThat(this.fooAdvice.adviceCalled).isEqualTo(2);
		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class))
				.isTrue();
		assertThat(TestUtils.getPropertyValue(this.store, "metadata", Map.class).size()).isEqualTo(1);

		message = new GenericMessage<>("bar");
		for (int i = 0; i < 2; i++) {
			this.input2.send(message);
			receive = this.output.receive(10000);
			assertThat(receive).isNotNull();
		}

		assertThat(receive.getHeaders().get(IntegrationMessageHeaderAccessor.DUPLICATE_MESSAGE, Boolean.class))
				.isTrue();
		assertThat(TestUtils.getPropertyValue(this.store, "metadata", Map.class).size()).isEqualTo(2);
		assertThat(this.store.get("bar")).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.store2, "metadata", Map.class).size()).isEqualTo(1);
		assertThat(this.store2.get("BAR")).isNotNull();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		private int adviceCalled;

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
