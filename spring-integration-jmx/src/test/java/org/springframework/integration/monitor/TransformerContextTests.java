/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.monitor;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class TransformerContextTests {

	private static volatile int adviceCalled;

	private static volatile int bazCalled;

	@Autowired
	private ApplicationContext context;

	@Test
	public void methodInvokingTransformer() {
		MessageChannel input = this.context.getBean("input", MessageChannel.class);
		PollableChannel output = this.context.getBean("output", PollableChannel.class);
		input.send(new GenericMessage<>("foo"));
		Message<?> reply = output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("FOO");
		assertThat(adviceCalled).isEqualTo(1);

		input = this.context.getBean("direct", MessageChannel.class);
		input.send(new GenericMessage<>("foo"));
		reply = output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("FOO");

		input = this.context.getBean("directRef", MessageChannel.class);
		input.send(new GenericMessage<>("foo"));
		reply = output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("FOO");
		assertThat(adviceCalled).isEqualTo(2);

		input = this.context.getBean("service", MessageChannel.class);
		input.send(new GenericMessage<>("foo"));
		assertThat(bazCalled).isEqualTo(1);
		assertThat(adviceCalled).isEqualTo(3);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

	public static class Bar extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			return MessageBuilder.withPayload(requestMessage.getPayload().toString().toUpperCase())
					.setHeader("callStack", st);
		}

	}

	public static class BazService {

		public void qux() {
			bazCalled++;
		}

		public String upperCase(String input) {
			return input.toUpperCase();
		}

	}

}
