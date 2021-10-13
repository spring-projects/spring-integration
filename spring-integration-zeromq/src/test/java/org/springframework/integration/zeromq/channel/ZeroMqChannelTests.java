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

package org.springframework.integration.zeromq.channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.zeromq.SocketType;
import org.zeromq.ZAuth;
import org.zeromq.ZCert;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import reactor.core.publisher.Mono;

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
		channel.setConsumeDelay(Duration.ofMillis(10));
		channel.setSendSocketConfigurer(socket -> socket.setZapDomain("global"));
		channel.setSubscribeSocketConfigurer(socket -> socket.setZapDomain("local"));
		AtomicBoolean customMessageMapperCalled = new AtomicBoolean();
		channel.setMessageMapper(new EmbeddedJsonHeadersMessageMapper() {

			@Override public Message<?> toMessage(byte[] bytes, Map<String, Object> headers) {
				customMessageMapperCalled.set(true);
				return super.toMessage(bytes, headers);
			}

		});
		channel.afterPropertiesSet();

		@SuppressWarnings("unchecked")
		Mono<ZMQ.Socket> sendSocketMono = TestUtils.getPropertyValue(channel, "sendSocket", Mono.class);
		ZMQ.Socket sendSocket = sendSocketMono.block(Duration.ofSeconds(10));
		assertThat(sendSocket.getZapDomain()).isEqualTo("global");

		@SuppressWarnings("unchecked")
		Mono<ZMQ.Socket> subscribeSocketMono = TestUtils.getPropertyValue(channel, "subscribeSocket", Mono.class);
		ZMQ.Socket subscribeSocket = subscribeSocketMono.block(Duration.ofSeconds(10));
		assertThat(subscribeSocket.getZapDomain()).isEqualTo("local");

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

		assertThat(customMessageMapperCalled.get()).isTrue();
	}

	@Test
	void testPubSubLocal() throws InterruptedException {
		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT, true);
		channel.setBeanName("testChannel2");
		channel.setConsumeDelay(Duration.ofMillis(10));
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
	void testPushPullBind() throws InterruptedException {
		ZeroMqProxy proxy = new ZeroMqProxy(CONTEXT);
		proxy.setBeanName("pullPushProxy");
		proxy.setExposeCaptureSocket(true);
		proxy.afterPropertiesSet();
		proxy.start();

		await().until(() -> proxy.getBackendPort() > 0);

		ZMQ.Socket captureSocket = CONTEXT.createSocket(SocketType.SUB);
		captureSocket.subscribe(ZMQ.SUBSCRIPTION_ALL);
		captureSocket.connect(proxy.getCaptureAddress());

		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT);
		channel.setConnectUrl("tcp://localhost:" + proxy.getFrontendPort() + ':' + proxy.getBackendPort());
		channel.setBeanName("testChannel3");
		channel.setConsumeDelay(Duration.ofMillis(10));
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

		byte[] recv = captureSocket.recv();
		assertThat(recv).isNotNull();
		Message<?> capturedMessage = new EmbeddedJsonHeadersMessageMapper().toMessage(recv);
		assertThat(capturedMessage).isEqualTo(testMessage);
		captureSocket.close();

		proxy.stop();
	}


	@Test
	void testPubSubBind() throws InterruptedException {
		ZeroMqProxy proxy = new ZeroMqProxy(CONTEXT, ZeroMqProxy.Type.SUB_PUB);
		proxy.setBeanName("subPubProxy");
		proxy.afterPropertiesSet();
		proxy.start();

		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT, true);
		channel.setZeroMqProxy(proxy);
		channel.setBeanName("testChannel4");
		channel.setConsumeDelay(Duration.ofMillis(10));
		channel.afterPropertiesSet();

		BlockingQueue<Message<?>> received = new LinkedBlockingQueue<>();

		channel.subscribe(received::offer);
		channel.subscribe(received::offer);

		await().until(() -> proxy.getBackendPort() > 0);

		ZeroMqChannel channel2 = new ZeroMqChannel(CONTEXT, true);
		channel2.setConnectUrl("tcp://localhost:" + proxy.getFrontendPort() + ':' + proxy.getBackendPort());
		channel2.setBeanName("testChannel5");
		channel2.setConsumeDelay(Duration.ofMillis(10));
		channel2.afterPropertiesSet();

		channel2.subscribe(received::offer);

		// Give it some time to connect and subscribe
		Thread.sleep(1000);

		GenericMessage<String> testMessage = new GenericMessage<>("test1");
		assertThat(channel.send(testMessage)).isTrue();

		Message<?> message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);
		message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);
		message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);
		assertThat(received.poll(100, TimeUnit.MILLISECONDS)).isNull();

		channel.destroy();
		channel2.destroy();
		proxy.stop();
	}

	@Test
	void testPubSubWithCurve() throws InterruptedException {
		new ZAuth(CONTEXT).configureCurve(ZAuth.CURVE_ALLOW_ANY);

		ZMQ.Curve.KeyPair frontendKeyPair = ZMQ.Curve.generateKeyPair();
		ZMQ.Curve.KeyPair backendKeyPair = ZMQ.Curve.generateKeyPair();

		ZeroMqProxy proxy = new ZeroMqProxy(CONTEXT, ZeroMqProxy.Type.SUB_PUB);
		proxy.setBeanName("subPubCurveProxy");
		proxy.setFrontendSocketConfigurer(socket -> {
			socket.setZAPDomain("global".getBytes());
			socket.setCurveServer(true);
			socket.setCurvePublicKey(frontendKeyPair.publicKey.getBytes());
			socket.setCurveSecretKey(frontendKeyPair.secretKey.getBytes());
		});
		proxy.setBackendSocketConfigurer(socket -> {
			socket.setZAPDomain("global".getBytes());
			socket.setCurveServer(true);
			socket.setCurvePublicKey(backendKeyPair.publicKey.getBytes());
			socket.setCurveSecretKey(backendKeyPair.secretKey.getBytes());
		});
		proxy.afterPropertiesSet();
		proxy.start();

		ZeroMqChannel channel = new ZeroMqChannel(CONTEXT, true);
		channel.setZeroMqProxy(proxy);
		channel.setBeanName("testChannelWithCurve");
		channel.setSendSocketConfigurer(socket -> {
			ZCert clientCert = new ZCert();
			socket.setCurvePublicKey(clientCert.getPublicKey());
			socket.setCurveSecretKey(clientCert.getSecretKey());
			socket.setCurveServerKey(frontendKeyPair.publicKey.getBytes());
		});
		channel.setSubscribeSocketConfigurer(socket -> {
					ZCert clientCert = new ZCert();
					socket.setCurvePublicKey(clientCert.getPublicKey());
					socket.setCurveSecretKey(clientCert.getSecretKey());
					socket.setCurveServerKey(backendKeyPair.publicKey.getBytes());
				}
		);
		channel.setConsumeDelay(Duration.ofMillis(10));
		channel.afterPropertiesSet();

		BlockingQueue<Message<?>> received = new LinkedBlockingQueue<>();

		channel.subscribe(received::offer);
		channel.subscribe(received::offer);

		await().until(() -> proxy.getBackendPort() > 0);

		// Give it some time to connect and subscribe
		Thread.sleep(1000);

		GenericMessage<String> testMessage = new GenericMessage<>("test1");
		assertThat(channel.send(testMessage)).isTrue();

		Message<?> message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);
		message = received.poll(10, TimeUnit.SECONDS);
		assertThat(message).isNotNull().isEqualTo(testMessage);

		channel.destroy();
		proxy.stop();
	}

}
