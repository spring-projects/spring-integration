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

package org.springframework.integration.zeromq.channel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zeromq.Utils;
import org.zeromq.ZContext;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ZeroMqChannelTests {

	private static final ZContext CONTEXT = new ZContext();

	@AfterAll
	static void tearDown() {
		CONTEXT.close();
	}

	@Test
	void testSimpleSendAndReceive() throws InterruptedException {
		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT);
		channel.setBeanName("testChannel1");
		channel.afterPropertiesSet();

		BlockingQueue<Message<?>> received = new LinkedBlockingQueue<>();

		channel.subscribe(received::offer);

		assertThat(channel.send(new GenericMessage<>("test1"), 1000)).isTrue();
		assertThat(channel.send(new GenericMessage<>("test2"), 500)).isTrue();

		Message<?> message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().extracting(Message::getPayload).isEqualTo("test1");
		message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().extracting(Message::getPayload).isEqualTo("test2");

		// Ensure that second subscriber doesn't make it as pub-sub
		channel.subscribe(received::offer);

		assertThat(channel.send(new GenericMessage<>("test3"))).isTrue();

		message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().extracting(Message::getPayload).isEqualTo("test3");
		assertThat(received.poll(100, TimeUnit.MILLISECONDS)).isNull();

		channel.destroy();
	}

	@Test
	void testPubSubLocal() throws InterruptedException {
		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT, true);
		channel.setBeanName("testChannel2");
		channel.afterPropertiesSet();

		BlockingQueue<Message<?>> received = new LinkedBlockingQueue<>();

		channel.subscribe(received::offer);
		channel.subscribe(received::offer);

		GenericMessage<String> testMessage = new GenericMessage<>("test1");
		assertThat(channel.send(testMessage)).isTrue();

		Message<?> message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);
		message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);

		channel.destroy();
	}

	@Test
	void testPushPullBind() throws InterruptedException, IOException {
		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT);
		channel.setBindUrl("tcp://*:" + Utils.findOpenPort() + ':' + Utils.findOpenPort());
		channel.setBeanName("testChannel3");
		channel.afterPropertiesSet();

		BlockingQueue<Message<?>> received = new LinkedBlockingQueue<>();

		channel.subscribe(received::offer);
		channel.subscribe(received::offer);

		GenericMessage<String> testMessage = new GenericMessage<>("test1");
		assertThat(channel.send(testMessage)).isTrue();

		Message<?> message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);
		assertThat(received.poll(100, TimeUnit.MILLISECONDS)).isNull();

		channel.destroy();
	}


	@Test
	void testPubSubBind() throws InterruptedException, IOException {
		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT, true);
		channel.setBindUrl("tcp://*:" + Utils.findOpenPort() + ':' + Utils.findOpenPort());
		channel.setBeanName("testChannel4");
		channel.afterPropertiesSet();

		BlockingQueue<Message<?>> received = new LinkedBlockingQueue<>();

		channel.subscribe(received::offer);
		channel.subscribe(received::offer);

		GenericMessage<String> testMessage = new GenericMessage<>("test1");
		assertThat(channel.send(testMessage)).isTrue();

		Message<?> message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);
		message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);

		channel.destroy();
	}

}
