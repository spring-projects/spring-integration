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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mapping.ConvertingBytesMessageMapper;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.support.converter.ConfigurableCompositeMessageConverter;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.zeromq.ZeroMqHeaders;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * A {@link MessageProducerSupport} implementation for consuming messages from ZeroMq socket.
 * Only {@link SocketType#PAIR}, {@link SocketType#SUB} and {@link SocketType#PULL} are supported.
 * This component can bind or connect the socket.
 * <p>
 * When the {@link SocketType#SUB} is used, the received topic is stored in the {@link ZeroMqHeaders#TOPIC}.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
@ManagedResource
@IntegrationManagedResource
public class ZeroMqMessageProducer extends MessageProducerSupport {

	public static final Duration DEFAULT_CONSUME_DELAY = Duration.ofSeconds(1);

	private static final List<SocketType> VALID_SOCKET_TYPES =
			Arrays.asList(SocketType.PAIR, SocketType.PULL, SocketType.SUB);

	private final Scheduler consumerScheduler = Schedulers.newSingle("zeroMqMessageProducerScheduler");

	private final AtomicInteger bindPort = new AtomicInteger();

	private final ZContext context;

	private final SocketType socketType;

	private InboundMessageMapper<byte[]> messageMapper;

	private Consumer<ZMQ.Socket> socketConfigurer = (socket) -> { };

	private Duration consumeDelay = DEFAULT_CONSUME_DELAY;

	private String[] topics = { "" }; // Equivalent to ZMQ#SUBSCRIPTION_ALL

	private boolean receiveRaw;

	@Nullable
	private String connectUrl;

	private volatile Mono<ZMQ.Socket> socketMono;

	public ZeroMqMessageProducer(ZContext context) {
		this(context, SocketType.PAIR);
	}

	public ZeroMqMessageProducer(ZContext context, SocketType socketType) {
		Assert.notNull(context, "'context' must not be null");
		Assert.state(VALID_SOCKET_TYPES.contains(socketType),
				() -> "'socketType' can only be one of the: " + VALID_SOCKET_TYPES);
		this.context = context;
		this.socketType = socketType;
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
	 * Provide an {@link InboundMessageMapper} to convert a consumed data into a message to produce.
	 * Ignored when {@link #setReceiveRaw(boolean)} is {@code true}.
	 * @param messageMapper the {@link InboundMessageMapper} to use.
	 */
	public void setMessageMapper(InboundMessageMapper<byte[]> messageMapper) {
		Assert.notNull(messageMapper, "'messageMapper' must not be null");
		this.messageMapper = messageMapper;
	}

	/**
	 * Provide a {@link MessageConverter} (as an alternative to {@link #messageMapper})
	 * for converting a consumed data into a message to produce.
	 * Ignored when {@link #setReceiveRaw(boolean)} is {@code true}.
	 * @param messageConverter the {@link MessageConverter} to use.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		setMessageMapper(new ConvertingBytesMessageMapper(messageConverter));
	}

	/**
	 * Whether raw {@link ZMsg} is present as a payload of message to produce or
	 * it is fully converted to a {@link Message} including {@link ZeroMqHeaders#TOPIC} header (if any).
	 * @param receiveRaw to convert from {@link ZMsg} or not; defaults to convert.
	 */
	public void setReceiveRaw(boolean receiveRaw) {
		this.receiveRaw = receiveRaw;
	}

	/**
	 * Provide a {@link Consumer} to configure a socket with arbitrary options, like security.
	 * @param socketConfigurer the configurer for socket options.
	 */
	public void setSocketConfigurer(Consumer<ZMQ.Socket> socketConfigurer) {
		Assert.notNull(socketConfigurer, "'socketConfigurer' must not be null");
		this.socketConfigurer = socketConfigurer;
	}

	/**
	 * Specify topics the {@link SocketType#SUB} socket is going to use for subscription.
	 * It is ignored for all other {@link SocketType}s supported.
	 * @param topics the topics to use.
	 */
	public void setTopics(String... topics) {
		Assert.notNull(topics, "'topics' cannot be null");
		Assert.noNullElements(topics, "'topics' must not contain null elements");
		this.topics = Arrays.copyOf(topics, topics.length);
	}

	/**
	 * Configure an URL for {@link org.zeromq.ZMQ.Socket#connect(String)}.
	 * Mutually exclusive with the {@link #setBindPort(int)}.
	 * @param connectUrl the URL to connect ZeroMq socket to.
	 */
	public void setConnectUrl(@Nullable String connectUrl) {
		this.connectUrl = connectUrl;
	}

	/**
	 * Configure a port for TCP protocol binding via {@link org.zeromq.ZMQ.Socket#bind(String)}.
	 * Mutually exclusive with the {@link #setConnectUrl(String)}.
	 * @param port the port to bind ZeroMq socket to over TCP.
	 */
	public void setBindPort(int port) {
		Assert.isTrue(port > 0, "'port' must not be zero or negative");
		this.bindPort.set(port);
	}

	/**
	 * Return the port a socket is bound or 0 if this message producer has not been started yet
	 * or the socket is connected - not bound.
	 * @return the port for a socket or 0.
	 */
	public int getBoundPort() {
		return this.bindPort.get();
	}

	@Override
	public String getComponentType() {
		return "zeromq:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state(this.connectUrl == null || this.bindPort.get() == 0,
				"Only one of the 'connectUrl' or `bindPort` must be provided on none");
		if (this.messageMapper == null && !this.receiveRaw) {
			ConfigurableCompositeMessageConverter messageConverter = new ConfigurableCompositeMessageConverter();
			messageConverter.setBeanFactory(getBeanFactory());
			messageConverter.afterPropertiesSet();
			this.messageMapper = new ConvertingBytesMessageMapper(messageConverter);
		}
	}

	@ManagedOperation
	public void subscribeToTopics(String... topics) {
		Assert.state(SocketType.SUB.equals(this.socketType), "Only SUB socket can accept a subscription option.");
		Assert.state(isActive(), "This message producer is not active to accept a new subscription.");

		Flux.fromArray(topics)
				.flatMap((topic) ->
						this.socketMono.doOnNext((socket) -> socket.subscribe(topic)))
				.subscribe();
	}

	@ManagedOperation
	public void unsubscribeFromTopics(String... topics) {
		Assert.state(SocketType.SUB.equals(this.socketType), "Only SUB socket can accept a unsubscription option.");
		Assert.state(isActive(), "This message producer is not active to cancel a subscription.");

		Flux.fromArray(topics)
				.flatMap((topic) ->
						this.socketMono.doOnNext((socket) -> socket.unsubscribe(topic)))
				.subscribe();
	}

	@Override
	protected void doStart() {
		this.socketMono =
				Mono.just(this.context.createSocket(this.socketType))
						.publishOn(this.consumerScheduler)
						.doOnNext(this.socketConfigurer)
						.doOnNext((socket) -> {
							if (SocketType.SUB.equals(this.socketType)) {
								for (String topic : this.topics) {
									socket.subscribe(topic);
								}
							}
						})
						.doOnNext((socket) -> {
							if (this.connectUrl != null) {
								socket.connect(this.connectUrl);
							}
							else {
								this.bindPort.set(bindSocket(socket, this.bindPort.get()));
							}
						})
						.cache()
						.publishOn(this.consumerScheduler);

		Flux<? extends Message<?>> dataFlux =
				this.socketMono
						.flatMap((socket) -> {
							if (isRunning()) {
								ZMsg msg = ZMsg.recvMsg(socket, false);
								if (msg != null) {
									return Mono.just(msg);
								}
							}
							return Mono.empty();
						})
						.publishOn(Schedulers.boundedElastic())
						.transform((msgMono) -> this.receiveRaw ? mapRaw(msgMono) : convertMessage(msgMono))
						.doOnError((error) ->
								logger.error(error, () -> "Error processing ZeroMQ message in the " + this))
						.repeatWhenEmpty((repeat) ->
								isActive() ? repeat.delayElements(this.consumeDelay) : repeat)
						.repeat(this::isActive)
						.doOnComplete(this.consumerScheduler::dispose);

		subscribeToPublisher(dataFlux);
	}

	private Mono<Message<?>> mapRaw(Mono<ZMsg> msgMono) {
		return msgMono.map((msg) -> getMessageBuilderFactory().withPayload(msg).build());
	}

	private Mono<Message<?>> convertMessage(Mono<ZMsg> msgMono) {
		return msgMono.map((msg) -> {
			Map<String, Object> headers = null;
			if (msg.size() > 1) {
				headers = Collections.singletonMap(ZeroMqHeaders.TOPIC, msg.unwrap().getString(ZMQ.CHARSET));
			}
			return this.messageMapper.toMessage(msg.getLast().getData(), headers); // NOSONAR
		});
	}

	@Override
	protected void doStop() {
		this.socketMono.doOnNext(ZMQ.Socket::close).subscribe();
	}

	@Override
	public void destroy() {
		super.destroy();
		this.socketMono.doOnNext(ZMQ.Socket::close).block();
	}

	private static int bindSocket(ZMQ.Socket socket, int port) {
		if (port == 0) {
			return socket.bindToRandomPort("tcp://*");
		}
		else {
			boolean bound = socket.bind("tcp://*:" + port);
			if (!bound) {
				throw new IllegalArgumentException("Cannot bind ZeroMQ socket to port: " + port);
			}
			return port;
		}
	}

}
