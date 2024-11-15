/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.transformer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Also in JMX - changes here should be reflected there.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class TransformerContextTests {

	private static volatile int adviceCalled;

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel direct;

	@Autowired
	private MessageChannel directRef;

	@Autowired
	private PollableChannel output;

	@Autowired
	private AbstractEndpoint pojoTransformer;

	@Autowired
	private TestBean testBean;

	@Test
	public void methodInvokingTransformer() {
		this.input.send(new GenericMessage<>("foo"));
		Message<?> reply = this.output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("FOO");
		assertThat(adviceCalled).isEqualTo(1);

		this.direct.send(new GenericMessage<>("foo"));
		reply = this.output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("FOO");
		StackTraceElement[] st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertThat(st[7].getMethodName()).isEqualTo("doSend"); // no MethodInvokerHelper

		this.directRef.send(new GenericMessage<>("foo"));
		reply = this.output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("FOO");
		st = (StackTraceElement[]) reply.getHeaders().get("callStack");
		assertThat(st[7].getMethodName()).isEqualTo("doSend"); // no MethodInvokerHelper

		assertThat(this.testBean.isRunning()).isTrue();
		this.pojoTransformer.stop();
		assertThat(this.testBean.isRunning()).isFalse();
		this.pojoTransformer.start();
		assertThat(this.testBean.isRunning()).isTrue();

		this.directRef.send(new GenericMessage<>("bar"));
		assertThat(this.output.receive(0)).isNull();
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
			if ("bar".equals(requestMessage.getPayload())) {
				return null;
			}
			Exception e = new RuntimeException();
			StackTraceElement[] st = e.getStackTrace();
			return MessageBuilder.withPayload(requestMessage.getPayload().toString().toUpperCase())
					.setHeader("callStack", st);
		}

	}

}
