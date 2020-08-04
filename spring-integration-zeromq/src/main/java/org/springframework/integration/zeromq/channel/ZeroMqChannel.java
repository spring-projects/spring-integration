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

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ZeroMqChannel extends AbstractMessageChannel implements SubscribableChannel {

	private final Map<MessageHandler, Disposable> subscribers = new HashMap<>();

	private final Scheduler publisherScheduler = Schedulers.newSingle("publisherScheduler");

	private final Scheduler subscriberScheduler = Schedulers.newSingle("publisherScheduler");

	private final ZContext context;

	private final boolean pubSub;

	private final Mono<ZMQ.Socket> sendSocket;

	private final Mono<ZMQ.Socket> subscribeSocket;

	private final Flux<? extends Message<?>> subscriberData;

	private BytesMessageMapper messageMapper = new EmbeddedJsonHeadersMessageMapper();

	@Nullable
	private String connectSendUrl;

	@Nullable
	private String connectSubscribeUrl;

	@Nullable
	private String bindInUrl;

	@Nullable
	private String bindOutUrl;

	private volatile boolean initialized;

	public ZeroMqChannel(ZContext context) {
		this(context, false);
	}

	public ZeroMqChannel(ZContext context, boolean pubSub) {
		Assert.notNull(context, "'context' must not be null");
		this.context = context;
		this.pubSub = pubSub;

		this.sendSocket =
				Mono.just(this.context.createSocket(this.pubSub ? SocketType.PUB : SocketType.PUSH))
						.publishOn(this.publisherScheduler)
						.doOnNext((socket) -> socket.connect(this.connectSendUrl))
						.cache();

		this.subscribeSocket =
				Mono.just(this.context.createSocket(this.pubSub ? SocketType.SUB : SocketType.PULL))
						.publishOn(this.subscriberScheduler)
						.doOnNext((socket) -> {
							socket.connect(this.connectSubscribeUrl);
							if (this.pubSub) {
								socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
							}
						})
						.cache();

		Flux<? extends Message<?>> receiveData =
				this.subscribeSocket
						.<byte[]>handle((socket, sink) -> {
							byte[] data = socket.recv(ZMQ.NOBLOCK);
							if (data != null) {
								sink.next(data);
							}
						})
						.publishOn(Schedulers.parallel())
						.map(this.messageMapper::toMessage)
						.doOnError((error) -> logger.error("Error processing ZeroMQ message", error))
						.repeatWhenEmpty((repeat) -> repeat.delayElements(Duration.ofMillis(100)))
						.repeat(() -> this.initialized)
						.retry();

		if (this.pubSub) {
			receiveData = receiveData.publish().autoConnect();
		}

		this.subscriberData = receiveData;

	}

	public void setConnectUrl(@Nullable String connectUrl) {
		if (connectUrl != null) {
			this.connectSendUrl = connectUrl.substring(0, connectUrl.lastIndexOf(':'));
			this.connectSubscribeUrl =
					this.connectSendUrl.substring(0, this.connectSendUrl.lastIndexOf(':'))
							+ connectUrl.substring(connectUrl.lastIndexOf(':'));
		}
	}

	public void setBindUrl(@Nullable String bindUrl) {
		if (bindUrl != null) {
			this.bindInUrl = bindUrl.substring(0, bindUrl.lastIndexOf(':'));
			this.bindOutUrl =
					this.bindInUrl.substring(0, this.bindInUrl.lastIndexOf(':'))
							+ bindUrl.substring(bindUrl.lastIndexOf(':'));
		}
	}

	public void setMessageMapper(BytesMessageMapper messageMapper) {
		Assert.notNull(messageMapper, "'messageMapper' must not be null");
		this.messageMapper = messageMapper;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.connectSendUrl == null) {
			if (this.bindInUrl == null) {
				this.bindInUrl = "inproc://" + getComponentName() + ".in";
				this.bindOutUrl = "inproc://" + getComponentName() + ".out";
				this.connectSendUrl = this.bindInUrl;
				this.connectSubscribeUrl = this.bindOutUrl;
			}

			Executors.newSingleThreadExecutor()
					.submit(() -> {
						try (ZMQ.Socket inSocket =
									 this.context.createSocket(this.pubSub ? SocketType.XSUB : SocketType.PULL);
							 ZMQ.Socket outSocket =
									 this.context.createSocket(this.pubSub ? SocketType.XPUB : SocketType.PUSH);
							 ZMQ.Socket controlSocket = this.context.createSocket(SocketType.PAIR)) {
							inSocket.bind(this.bindInUrl);
							outSocket.bind(this.bindOutUrl);
							controlSocket.bind("inproc://" + getComponentName() + ".control");
							zmq.ZMQ.proxy(inSocket.base(), outSocket.base(), null, controlSocket.base());
						}
					});
		}

		this.initialized = true;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		Assert.state(this.initialized, "the channel is not initialized yet or already destroyed");

		byte[] data = this.messageMapper.fromMessage(message);
		Assert.state(data != null, () -> "The '" + this.messageMapper + "' returned null for '" + message + '\'');

		Boolean sent =
				this.sendSocket
						.map((socket) -> socket.send(data))
						.block(Duration.ofMillis(timeout));

		return Boolean.TRUE.equals(sent);
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		Assert.state(this.initialized, "the channel is not initialized yet or already destroyed");
		this.subscribers.computeIfAbsent(handler, (key) -> this.subscriberData.subscribe(handler::handleMessage));
		return true;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		Disposable disposable = this.subscribers.remove(handler);
		if (disposable != null) {
			disposable.dispose();
			return true;
		}
		return false;
	}

	@Override
	public void destroy() {
		this.initialized = false;
		super.destroy();
		try (ZMQ.Socket commandSocket = context.createSocket(SocketType.PAIR)) {
			commandSocket.connect("inproc://" + getComponentName() + ".control");
			commandSocket.send(zmq.ZMQ.PROXY_TERMINATE);
		}
		this.sendSocket.doOnNext(ZMQ.Socket::close).block();
		this.publisherScheduler.dispose();
		HashSet<MessageHandler> handlersCopy = new HashSet<>(this.subscribers.keySet());
		handlersCopy.forEach(this::unsubscribe);
		this.subscribeSocket.doOnNext(ZMQ.Socket::close).block();
		this.subscriberScheduler.dispose();
	}

}
