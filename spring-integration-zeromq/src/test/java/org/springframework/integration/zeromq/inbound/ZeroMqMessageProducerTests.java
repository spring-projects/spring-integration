/*
 * Copyright 2020-2021 the original author or authors.
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
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import java.time.Duration;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.support.GenericMessage;

import reactor.core.publisher.Mono;
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
		FluxMessageChannel outputChannel = new FluxMessageChannel();

		StepVerifier stepVerifier =
				StepVerifier.create(outputChannel)
						.assertNext((message) -> assertThat(message.getPayload()).isEqualTo("test"))
						.assertNext((message) -> assertThat(message.getPayload()).isEqualTo("test2"))
						.thenCancel()
						.verifyLater();

		ZeroMqMessageProducer messageProducer = new ZeroMqMessageProducer(CONTEXT);
		messageProducer.setOutputChannel(outputChannel);
		messageProducer.setMessageMapper((object, headers) -> new GenericMessage<>(new String(object)));
		messageProducer.setConsumeDelay(Duration.ofMillis(10));
		messageProducer.setBeanFactory(mock(BeanFactory.class));
		messageProducer.setSocketConfigurer(s -> s.setZapDomain("global"));
		messageProducer.afterPropertiesSet();
		messageProducer.start();

		@SuppressWarnings("unchecked")
		Mono<ZMQ.Socket> socketMono = TestUtils.getPropertyValue(messageProducer, "socketMono", Mono.class);
		ZMQ.Socket socketInUse = socketMono.block(Duration.ofSeconds(10));
		assertThat(socketInUse.getZapDomain()).isEqualTo("global");

		ZMQ.Socket socket = CONTEXT.createSocket(SocketType.PAIR);

		await().until(() -> messageProducer.getBoundPort() > 0);

		socket.connect("tcp://localhost:" + messageProducer.getBoundPort());

		socket.send("test");
		socket.send("test2");

		stepVerifier.verify();

		messageProducer.destroy();
		socket.close();
	}

	@Test
	void testMessageProducerForPubSubReceiveRaw() throws InterruptedException {
		String socketAddress = "inproc://messageProducer.test";
		ZMQ.Socket socket = CONTEXT.createSocket(SocketType.PUB);
		socket.bind(socketAddress);

		FluxMessageChannel outputChannel = new FluxMessageChannel();

		StepVerifier stepVerifier =
				StepVerifier.create(outputChannel)
						.assertNext((message) ->
								assertThat(message.getPayload())
										.asInstanceOf(InstanceOfAssertFactories.type(ZMsg.class))
										.extracting(ZMsg::unwrap)
										.isEqualTo(new ZFrame("testTopic")))
						.assertNext((message) ->
								assertThat(message.getPayload())
										.asInstanceOf(InstanceOfAssertFactories.type(ZMsg.class))
										.extracting(ZMsg::unwrap)
										.isEqualTo(new ZFrame("otherTopic")))
						.thenCancel()
						.verifyLater();

		ZeroMqMessageProducer messageProducer = new ZeroMqMessageProducer(CONTEXT, SocketType.SUB);
		messageProducer.setOutputChannel(outputChannel);
		messageProducer.setTopics("test");
		messageProducer.setReceiveRaw(true);
		messageProducer.setConnectUrl(socketAddress);
		messageProducer.setConsumeDelay(Duration.ofMillis(10));
		messageProducer.setBeanFactory(mock(BeanFactory.class));
		messageProducer.afterPropertiesSet();
		messageProducer.start();

		// Give it some time to connect and subscribe
		Thread.sleep(2000);

		ZMsg msg = ZMsg.newStringMsg("test");
		msg.wrap(new ZFrame("testTopic"));
		msg.send(socket);

		messageProducer.subscribeToTopics("other");

		// Give it some time to connect and subscribe
		Thread.sleep(2000);

		msg = ZMsg.newStringMsg("test");
		msg.wrap(new ZFrame("otherTopic"));
		msg.send(socket);

		stepVerifier.verify(Duration.ofSeconds(10));

		messageProducer.destroy();
		socket.close();
	}

}
