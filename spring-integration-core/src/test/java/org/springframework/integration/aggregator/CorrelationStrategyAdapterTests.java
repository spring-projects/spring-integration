/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 *
 */
public class CorrelationStrategyAdapterTests implements TestApplicationContextAware {

	private Message<?> message;

	@BeforeEach
	public void init() {
		message = MessageBuilder.withPayload("foo").setHeader("a", "b").setHeader("c", "d").build();
	}

	@Test
	public void testMethodName() {
		MethodInvokingCorrelationStrategy adapter =
				new MethodInvokingCorrelationStrategy(new SimpleMessageCorrelator(), "getKey");
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(adapter.getCorrelationKey(message)).isEqualTo("b");
	}

	@Test
	public void testCorrelationStrategyAdapterObjectMethod() {
		MethodInvokingCorrelationStrategy adapter =
				new MethodInvokingCorrelationStrategy(new SimpleMessageCorrelator(),
						ReflectionUtils.findMethod(SimpleMessageCorrelator.class, "getKey", Message.class));
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(adapter.getCorrelationKey(message)).isEqualTo("b");
	}

	@Test
	public void testCorrelationStrategyAdapterPojoMethod() {
		MethodInvokingCorrelationStrategy adapter =
				new MethodInvokingCorrelationStrategy(new SimplePojoCorrelator(), "getKey");
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(adapter.getCorrelationKey(message)).isEqualTo("foo");
	}

	@Test
	public void testHeaderPojoMethod() {
		MethodInvokingCorrelationStrategy adapter =
				new MethodInvokingCorrelationStrategy(new SimpleHeaderCorrelator(), "getKey");
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(adapter.getCorrelationKey(message)).isEqualTo("b");
	}

	@Test
	public void testHeadersPojoMethod() {
		MethodInvokingCorrelationStrategy adapter = new MethodInvokingCorrelationStrategy(new MultiHeaderCorrelator(),
				ReflectionUtils.findMethod(MultiHeaderCorrelator.class, "getKey", String.class, String.class));
		adapter.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		assertThat(adapter.getCorrelationKey(message)).isEqualTo("bd");
	}

	private static class MultiHeaderCorrelator {

		MultiHeaderCorrelator() {
			super();
		}

		@SuppressWarnings("unused")
		public String getKey(@Header("a") String header, @Header("c") String other) {
			return header + other;
		}

	}

	private static class SimpleHeaderCorrelator {

		SimpleHeaderCorrelator() {
			super();
		}

		@SuppressWarnings("unused")
		public String getKey(@Header("a") String header) {
			return header;
		}

	}

	private static class SimplePojoCorrelator {

		SimplePojoCorrelator() {
			super();
		}

		@SuppressWarnings("unused")
		public String getKey(String message) {
			return message;
		}

	}

	private static class SimpleMessageCorrelator {

		SimpleMessageCorrelator() {
			super();
		}

		@SuppressWarnings("unused")
		public String getKey(Message<?> message) {
			return (String) message.getHeaders().get("a");
		}

	}

}
