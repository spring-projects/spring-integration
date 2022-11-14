/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.channel.interceptor;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class WireTapTests {

	@Test
	public void wireTapWithNoSelector() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		mainChannel.send(new GenericMessage<>("testing"));
		Message<?> original = mainChannel.receive(0);
		assertThat(original).isNotNull();
		Message<?> intercepted = secondaryChannel.receive(0);
		assertThat(intercepted).isNotNull();
		assertThat(intercepted).isEqualTo(original);
	}

	@Test
	public void wireTapWithRejectingSelector() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel, new TestSelector(false)));
		mainChannel.send(new GenericMessage<>("testing"));
		Message<?> original = mainChannel.receive(0);
		assertThat(original).isNotNull();
		Message<?> intercepted = secondaryChannel.receive(0);
		assertThat(intercepted).isNull();
	}

	@Test
	public void wireTapWithAcceptingSelector() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel, new TestSelector(true)));
		mainChannel.send(new GenericMessage<>("testing"));
		Message<?> original = mainChannel.receive(0);
		assertThat(original).isNotNull();
		Message<?> intercepted = secondaryChannel.receive(0);
		assertThat(intercepted).isNotNull();
		assertThat(intercepted).isEqualTo(original);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wireTapTargetMustNotBeNull() {
		new WireTap((MessageChannel) null);
	}

	@Test
	public void simpleTargetWireTap() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		assertThat(secondaryChannel.receive(0)).isNull();
		Message<?> message = new GenericMessage<>("testing");
		mainChannel.send(message);
		Message<?> original = mainChannel.receive(0);
		Message<?> intercepted = secondaryChannel.receive(0);
		assertThat(original).isNotNull();
		assertThat(intercepted).isNotNull();
		assertThat(intercepted).isEqualTo(original);
	}

	@Test
	public void interceptedMessageContainsHeaderValue() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		String headerName = "testAttribute";
		Message<String> message = MessageBuilder.withPayload("testing")
				.setHeader(headerName, 123).build();
		mainChannel.send(message);
		Message<?> original = mainChannel.receive(0);
		Message<?> intercepted = secondaryChannel.receive(0);
		Object originalAttribute = original.getHeaders().get(headerName);
		Object interceptedAttribute = intercepted.getHeaders().get(headerName);
		assertThat(originalAttribute).isNotNull();
		assertThat(interceptedAttribute).isNotNull();
		assertThat(interceptedAttribute).isEqualTo(originalAttribute);
	}

	@Test
	public void wireTapDoesNotInterceptItsOwnChannel() {
		QueueChannel wireTapChannel = new QueueChannel();
		wireTapChannel.addInterceptor(new WireTap(wireTapChannel));
		// would throw a StackOverflowException if not working:
		wireTapChannel.send(MessageBuilder.withPayload("test").build());
	}

	private static class TestSelector implements MessageSelector {

		private boolean shouldAccept;

		TestSelector(boolean shouldAccept) {
			this.shouldAccept = shouldAccept;
		}

		public boolean accept(Message<?> message) {
			return this.shouldAccept;
		}

	}

}
