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

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.integration.zeromq.ZeroMqProxy;
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
 * The {@link SubscribableChannel} implementation over ZeroMQ sockets.
 * It can work in two messaging models:
 * - {@code push-pull}, where sent messages are distributed to subscribers in a round-robin manner
 * according a respective ZeroMQ {@link SocketType#PUSH} and {@link SocketType#PULL} socket types logic;
 * - {@code pub-sub}, where sent messages are distributed to all subscribers.
 * <p>
 * This message channel can work in local mode, when a pair of ZeroMQ sockets of {@link SocketType#PAIR} type
 * are connected between publisher (send operation) and subscriber using inter-thread transport binding.
 * <p>
 * In distributed mode this channel has to be connected to an externally managed ZeroMQ proxy.
 * The {@link #setConnectUrl(String)} has to be as a standard ZeroMQ connect string, but with an extra port
 * over the colon - representing a frontend and backend sockets pair on ZeroMQ proxy.
 * For example: {@code tcp://localhost:6001:6002}.
 * Another option is to provide a reference to the {@link ZeroMqProxy} instance managed in the same application:
 * frontend and backend ports are evaluated from this proxy and the respective connection string is built from them.
 * <p>
 * This way sending and receiving operations on this channel are similar to interaction over a messaging broker.
 * <p>
 * An internal logic of this message channel implementation is based on the project Reactor using its
 * {@link Mono}, {@link Flux} and {@link Scheduler} API for better thread model and flow control to avoid
 * concurrency primitives for multi-publisher(subscriber) communication within the same application.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ZeroMqChannel extends AbstractMessageChannel implements SubscribableChannel {

	public static final Duration DEFAULT_CONSUME_DELAY = Duration.ofSeconds(1);

	private final Map<MessageHandler, Disposable> subscribers = new HashMap<>();

	private final Scheduler publisherScheduler = Schedulers.newSingle("publisherScheduler");

	private final Scheduler subscriberScheduler = Schedulers.newSingle("subscriberScheduler");

	private final ZContext context;

	private final boolean pubSub;

	private final Mono<ZMQ.Socket> sendSocket;

	private final Mono<ZMQ.Socket> subscribeSocket;

	private final Flux<? extends Message<?>> subscriberData;

	private Duration consumeDelay = DEFAULT_CONSUME_DELAY;

	private BytesMessageMapper messageMapper = new EmbeddedJsonHeadersMessageMapper();

	private Consumer<ZMQ.Socket> sendSocketConfigurer = (socket) -> { };

	private Consumer<ZMQ.Socket> subscribeSocketConfigurer = (socket) -> { };

	@Nullable
	private ZeroMqProxy zeroMqProxy;

	@Nullable
	private volatile String connectSendUrl;

	@Nullable
	private volatile String connectSubscribeUrl;

	@Nullable
	private volatile Disposable subscriberDataDisposable;

	private volatile boolean initialized;

	/**
	 * Create a channel instance based on the provided {@link ZContext} with push/pull
	 * communication model.
	 * @param context the {@link ZContext} to use.
	 */
	public ZeroMqChannel(ZContext context) {
		this(context, false);
	}

	/**
	 * Create a channel instance based on the provided {@link ZContext} and provided
	 * communication model.
	 * @param context the {@link ZContext} to use.
	 * @param pubSub the communication model: push/pull or pub/sub.
	 */
	public ZeroMqChannel(ZContext context, boolean pubSub) {
		Assert.notNull(context, "'context' must not be null");
		this.context = context;
		this.pubSub = pubSub;

		Supplier<String> localPairConnection = () -> "inproc://" + getComponentName() + ".pair";

		Mono<?> proxyMono = prepareProxyMono();
		this.sendSocket = prepareSendSocketMono(localPairConnection, proxyMono);
		this.subscribeSocket = prepareSubscribeSocketMono(localPairConnection, proxyMono);
		this.subscriberData = prepareSubscriberDataFlux();
	}

	private Mono<Integer> prepareProxyMono() {
		return Mono.defer(() -> {
			if (this.zeroMqProxy != null) {
				return Mono.fromCallable(() -> this.zeroMqProxy.getBackendPort())
						.filter((proxyPort) -> proxyPort > 0)
						.repeatWhenEmpty(100, (repeat) -> repeat.delayElements(Duration.ofMillis(100))) // NOSONAR
						.doOnNext((proxyPort) ->
								setConnectUrl("tcp://localhost:" + this.zeroMqProxy.getFrontendPort() +
										':' + this.zeroMqProxy.getBackendPort()))
						.doOnError((error) ->
								logger.error(error,
										() -> "The provided '" + this.zeroMqProxy + "' has not been started"));
			}
			else {
				return Mono.empty();
			}
		})
				.cache();
	}

	private Mono<ZMQ.Socket> prepareSendSocketMono(Supplier<String> localPairConnection, Mono<?> proxyMono) {
		return proxyMono.publishOn(this.publisherScheduler)
				.then(Mono.fromCallable(() ->
						this.context.createSocket(
								this.connectSendUrl == null
										? SocketType.PAIR
										: (this.pubSub ? SocketType.PUB : SocketType.PUSH))
				))
				.doOnNext((socket) -> this.sendSocketConfigurer.accept(socket))
				.doOnNext((socket) ->
						socket.connect(this.connectSendUrl != null
								? this.connectSendUrl
								: localPairConnection.get()))
				.cache()
				.publishOn(this.publisherScheduler);
	}

	private Mono<ZMQ.Socket> prepareSubscribeSocketMono(Supplier<String> localPairConnection, Mono<?> proxyMono) {
		return proxyMono.publishOn(this.subscriberScheduler)
				.then(Mono.fromCallable(() ->
						this.context.createSocket(
								this.connectSubscribeUrl == null
										? SocketType.PAIR
										: (this.pubSub ? SocketType.SUB : SocketType.PULL))))
				.doOnNext((socket) -> this.subscribeSocketConfigurer.accept(socket))
				.doOnNext((socket) -> {
					if (this.connectSubscribeUrl != null) {
						if (this.pubSub) {
							socket.subscribe(ZMQ.SUBSCRIPTION_ALL);
						}
						socket.connect(this.connectSubscribeUrl);
					}
					else {
						socket.bind(localPairConnection.get());
					}
				})
				.cache()
				.publishOn(this.subscriberScheduler);
	}

	private Flux<? extends Message<?>> prepareSubscriberDataFlux() {
		Flux<? extends Message<?>> receiveData =
				this.subscribeSocket
						.flatMap((socket) -> {
							if (this.initialized) {
								byte[] data = socket.recv(ZMQ.NOBLOCK);
								if (data != null) {
									return Mono.just(data);
								}
							}
							return Mono.empty();
						})
						.publishOn(Schedulers.parallel())
						.map((data) -> this.messageMapper.toMessage(data))
						.doOnError((error) -> logger.error(error,
								() -> "Error processing ZeroMQ message in the " + this))
						.repeatWhenEmpty((repeat) ->
								this.initialized
										? repeat.delayElements(this.consumeDelay)
										: repeat)
						.repeat(() -> this.initialized);

		if (this.pubSub) {
			receiveData =
					receiveData.publish()
							.autoConnect(1, (disposable) -> this.subscriberDataDisposable = disposable);
		}
		return receiveData;
	}

	/**
	 * Configure a connection to the ZeroMQ proxy with the pair of ports over colon
	 * for proxy frontend and backend sockets. Mutually exclusive with the {@link #setZeroMqProxy(ZeroMqProxy)}.
	 * @param connectUrl the connection string in format {@code PROTOCOL://HOST:FRONTEND_PORT:BACKEND_PORT},
	 *                    e.g. {@code tcp://localhost:6001:6002}
	 */
	public void setConnectUrl(@Nullable String connectUrl) {
		if (connectUrl != null) {
			this.connectSendUrl = connectUrl.substring(0, connectUrl.lastIndexOf(':'));
			this.connectSubscribeUrl =
					this.connectSendUrl.substring(0, this.connectSendUrl.lastIndexOf(':')) // NOSONAR
							+ connectUrl.substring(connectUrl.lastIndexOf(':'));
		}
	}

	/**
	 * Specify a reference to a {@link ZeroMqProxy} instance in the same application
	 * to rely on its ports configuration and make a natural lifecycle dependency without guessing
	 * when the proxy is started. Mutually exclusive with the {@link #setConnectUrl(String)}.
	 * @param zeroMqProxy the {@link ZeroMqProxy} instance to use
	 */
	public void setZeroMqProxy(@Nullable ZeroMqProxy zeroMqProxy) {
		this.zeroMqProxy = zeroMqProxy;
	}

	/**
	 * Specify a {@link Duration} to delay consumption when no data received.
	 * @param consumeDelay the {@link Duration} to delay consumption when empty;
	 *                     defaults to {@link #DEFAULT_CONSUME_DELAY}.
	 */
	public void setConsumeDelay(Duration consumeDelay) {
		Assert.notNull(consumeDelay, "'consumeDelay' must not be null");
		this.consumeDelay = consumeDelay;
	}

	/**
	 * Provide a {@link BytesMessageMapper} to convert to/from messages when send or receive happens
	 * on the sockets.
	 * @param messageMapper the {@link BytesMessageMapper} to use;
	 *                      defaults to {@link EmbeddedJsonHeadersMessageMapper}.
	 */
	public void setMessageMapper(BytesMessageMapper messageMapper) {
		Assert.notNull(messageMapper, "'messageMapper' must not be null");
		this.messageMapper = messageMapper;
	}

	/**
	 * The {@link Consumer} callback to configure a publishing socket.
	 * The send socket is connected to the frontend socket of ZeroMQ proxy (if any).
	 * @param sendSocketConfigurer the {@link Consumer} to use.
	 */
	public void setSendSocketConfigurer(Consumer<ZMQ.Socket> sendSocketConfigurer) {
		Assert.notNull(sendSocketConfigurer, "'sendSocketConfigurer' must not be null");
		this.sendSocketConfigurer = sendSocketConfigurer;
	}

	/**
	 * The {@link Consumer} callback to configure a consuming socket.
	 * The subscribe socket is connected to the backend socket of ZeroMQ proxy (if any).
	 * @param subscribeSocketConfigurer the {@link Consumer} to use.
	 */
	public void setSubscribeSocketConfigurer(Consumer<ZMQ.Socket> subscribeSocketConfigurer) {
		Assert.notNull(subscribeSocketConfigurer, "'subscribeSocketConfigurer' must not be null");
		this.subscribeSocketConfigurer = subscribeSocketConfigurer;
	}

	@Override
	protected void onInit() {
		Assert.state(this.zeroMqProxy == null || this.connectSendUrl == null,
				"A 'zeroMqProxy' or 'connectUrl' can be provided (or none), but not both.");
		super.onInit();
		this.sendSocket.subscribe();
		this.initialized = true;
	}

	@Override
	protected boolean doSend(Message<?> message, long timeout) {
		Assert.state(this.initialized, "the channel is not initialized yet or already destroyed");

		byte[] data = this.messageMapper.fromMessage(message);
		Assert.state(data != null, () -> "The '" + this.messageMapper + "' returned null for '" + message + '\'');

		Mono<Boolean> sendMono = this.sendSocket.map((socket) -> socket.send(data));
		Boolean sent =
				timeout > 0
						? sendMono.block(Duration.ofMillis(timeout))
						: sendMono.block();

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
		this.sendSocket.doOnNext(ZMQ.Socket::close).block();
		this.publisherScheduler.dispose();
		HashSet<MessageHandler> handlersCopy = new HashSet<>(this.subscribers.keySet());
		handlersCopy.forEach(this::unsubscribe);
		this.subscribeSocket.doOnNext(ZMQ.Socket::close).block();
		this.subscriberScheduler.dispose();
		if (this.subscriberDataDisposable != null) {
			this.subscriberDataDisposable.dispose(); // NOSONAR
		}
	}

}
