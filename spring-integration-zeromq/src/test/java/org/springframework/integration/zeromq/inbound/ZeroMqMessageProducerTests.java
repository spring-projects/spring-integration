/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.zeromq.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.messaging.support.GenericMessage;

import reactor.test.StepVerifier;

/**
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ZeroMqMessageProducerTests {

	private static final ZContext CONTEXT = new ZContext();

	@AfterAll
	static void tearDown() {
		CONTEXT.close();
	}

	@Test
	void testMessageProducerForPair() {
		String socketAddress = "inproc://messageProducer.test";

		FluxMessageChannel outputChannel = new FluxMessageChannel();

		StepVerifier stepVerifier =
				StepVerifier.create(outputChannel)
						.assertNext((message) -> assertThat(message.getPayload()).isEqualTo("test"))
						.assertNext((message) -> assertThat(message.getPayload()).isEqualTo("test2"))
						.thenCancel()
						.verifyLater();

		ZeroMqMessageProducer messageProducer = new ZeroMqMessageProducer(CONTEXT);
		messageProducer.setBindUrl(socketAddress);
		messageProducer.setOutputChannel(outputChannel);
		messageProducer.setMessageMapper((object, headers) -> new GenericMessage<>(new String(object)));
		messageProducer.setBeanFactory(mock(BeanFactory.class));
		messageProducer.afterPropertiesSet();
		messageProducer.start();

		ZMQ.Socket socket = CONTEXT.createSocket(SocketType.PAIR);
		socket.connect(socketAddress);

		socket.send("test");
		socket.send("test2");

		stepVerifier.verify();

		messageProducer.destroy();
		socket.close();
	}

}
