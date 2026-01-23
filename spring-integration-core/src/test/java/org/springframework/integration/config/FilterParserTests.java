/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class FilterParserTests {

	@Autowired
	@Qualifier("adapterInput")
	MessageChannel adapterInput;

	@Autowired
	@Qualifier("adapterOutput")
	PollableChannel adapterOutput;

	@Autowired
	@Qualifier("implementationInput")
	MessageChannel implementationInput;

	@Autowired
	@Qualifier("implementationOutput")
	PollableChannel implementationOutput;

	@Autowired
	@Qualifier("exceptionInput")
	MessageChannel exceptionInput;

	@Autowired
	@Qualifier("discardInput")
	MessageChannel discardInput;

	@Autowired
	@Qualifier("discardOutput")
	PollableChannel discardOutput;

	@Autowired
	@Qualifier("discardAndExceptionInput")
	MessageChannel discardAndExceptionInput;

	@Autowired
	@Qualifier("discardAndExceptionOutput")
	PollableChannel discardAndExceptionOutput;

	@Autowired
	@Qualifier("advised.handler")
	MessageFilter advised;

	@Autowired
	@Qualifier("notAdvised.handler")
	MessageFilter notAdvised;

	private static volatile int adviceCalled;

	@Test
	public void adviseDiscard() {
		assertThat(TestUtils.<Boolean>getPropertyValue(this.advised, "postProcessWithinAdvice")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(this.notAdvised, "postProcessWithinAdvice")).isTrue();
	}

	@Test
	public void filterWithSelectorAdapterAccepts() {
		adviceCalled = 0;
		adapterInput.send(new GenericMessage<>("test"));
		Message<?> reply = adapterOutput.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("test");
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void filterWithSelectorAdapterRejects() {
		adapterInput.send(new GenericMessage<>(""));
		Message<?> reply = adapterOutput.receive(0);
		assertThat(reply).isNull();
	}

	@Test
	public void filterWithSelectorImplementationAccepts() {
		implementationInput.send(new GenericMessage<>("test"));
		Message<?> reply = implementationOutput.receive(0);
		assertThat(reply).isNotNull();
		assertThat(reply.getPayload()).isEqualTo("test");
	}

	@Test
	public void filterWithSelectorImplementationRejects() {
		implementationInput.send(new GenericMessage<>(""));
		Message<?> reply = implementationOutput.receive(0);
		assertThat(reply).isNull();
	}

	@Test
	public void exceptionThrowingFilterAccepts() {
		exceptionInput.send(new GenericMessage<>("test"));
		Message<?> reply = implementationOutput.receive(0);
		assertThat(reply).isNotNull();
	}

	@Test
	public void exceptionThrowingFilterRejects() {
		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> exceptionInput.send(new GenericMessage<>("")));
	}

	@Test
	public void filterWithDiscardChannel() {
		discardInput.send(new GenericMessage<>(""));
		Message<?> discard = discardOutput.receive(0);
		assertThat(discard).isNotNull();
		assertThat(discard.getPayload()).isEqualTo("");
		assertThat(adapterOutput.receive(0)).isNull();
	}

	@Test
	public void filterWithDiscardChannelAndException() {
		assertThatExceptionOfType(MessageRejectedException.class)
				.isThrownBy(() -> discardAndExceptionInput.send(new GenericMessage<>("")));
		Message<?> discard = discardAndExceptionOutput.receive(0);
		assertThat(discard).isNotNull();
		assertThat(discard.getPayload()).isEqualTo("");
		assertThat(adapterOutput.receive(0)).isNull();
	}

	public static class TestSelectorBean {

		public boolean hasText(String s) {
			return StringUtils.hasText(s);
		}

	}

	public static class TestSelectorImpl implements MessageSelector {

		public boolean accept(Message<?> message) {
			if (message != null && message.getPayload() instanceof String) {
				return StringUtils.hasText((String) message.getPayload());
			}
			return false;
		}

	}

	public static class FooFilter extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
