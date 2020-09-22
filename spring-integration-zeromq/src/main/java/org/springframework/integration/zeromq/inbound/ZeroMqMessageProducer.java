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
import java.util.function.Consumer;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
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

	private final ZContext context;

	private final SocketType socketType;

	private InboundMessageMapper<byte[]> messageMapper;

	private Consumer<ZMQ.Socket> socketConfigurer = (socket) -> { };

	private Duration consumeDelay = DEFAULT_CONSUME_DELAY;

	private String[] topics = { "" }; // Equivalent to ZMQ#SUBSCRIPTION_ALL

	@Nullable
	private String connectUrl;

	@Nullable
	private String bindUrl;

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
	 * @param messageMapper the {@link InboundMessageMapper} to use.
	 */
	public void setMessageMapper(InboundMessageMapper<byte[]> messageMapper) {
		Assert.notNull(messageMapper, "'messageMapper' must not be null");
		this.messageMapper = messageMapper;
	}

	/**
	 * Provide a {@link MessageConverter} (as an alternative to {@link #messageMapper})
	 * for converting a consumed data into a message to produce.
	 * @param messageConverter the {@link MessageConverter} to use.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		setMessageMapper(new ConvertingBytesMessageMapper(messageConverter));
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

	public void setConnectUrl(@Nullable String connectUrl) {
		this.connectUrl = connectUrl;
	}

	public void setBindUrl(@Nullable String bindUrl) {
		this.bindUrl = bindUrl;
	}

	@Override
	public String getComponentType() {
		return "zeromq:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.state((this.connectUrl != null && this.bindUrl == null)
						|| (this.connectUrl == null && this.bindUrl != null),
				"Exactly only one of the 'connectUrl' or `bindUrl` must be provided");
		if (this.messageMapper == null) {
			ConfigurableCompositeMessageConverter messageConverter = new ConfigurableCompositeMessageConverter();
			messageConverter.setBeanFactory(getBeanFactory());
			messageConverter.afterPropertiesSet();
			this.messageMapper = new ConvertingBytesMessageMapper(messageConverter);
		}
	}

	@ManagedOperation
	public void subscribeToTopics(String... topics) {
		Assert.state(SocketType.SUB.equals(this.socketType), "Only SUB socket can accept a subscription option.");
		Assert.state(isRunning(), "This message producer is not active to accept a new subscription.");

		Flux.fromArray(topics)
				.flatMap((topic) ->
						this.socketMono.doOnNext((socket) -> socket.subscribe(topic)))
				.subscribe();
	}

	@ManagedOperation
	public void unsubscribeFromTopics(String... topics) {
		Assert.state(SocketType.SUB.equals(this.socketType), "Only SUB socket can accept a unsubscription option.");
		Assert.state(isRunning(), "This message producer is not active to cancel a subscription.");

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
								socket.bind(this.bindUrl);
							}
						})
						.cache()
						.publishOn(this.consumerScheduler);

		Flux<? extends Message<?>> dataFlux =
				this.socketMono.flatMap((socket) -> {
					if (isRunning()) {
						ZMsg msg = ZMsg.recvMsg(socket, false);
						if (msg != null) {
							return Mono.just(msg);
						}
					}
					return Mono.empty();
				})
						.publishOn(Schedulers.parallel())
						.map((msg) -> {
							ZFrame first = msg.getFirst();
							ZFrame last = msg.getLast();
							Map<String, Object> headers = null;
							if (!first.equals(last)) {
								headers = Collections.singletonMap(ZeroMqHeaders.TOPIC, first.getString(ZMQ.CHARSET));
							}
							return this.messageMapper.toMessage(last.getData(), headers); // NOSONAR
						})
						.doOnError((error) -> logger.error("Error processing ZeroMQ message", error))
						.repeatWhenEmpty((repeat) ->
								isRunning()
										? repeat.delayElements(this.consumeDelay)
										: repeat)
						.repeat(this::isRunning);

		subscribeToPublisher(dataFlux);
	}

	@Override
	protected void doStop() {
		this.socketMono.doOnNext(ZMQ.Socket::close).subscribe();
	}

	@Override
	public void destroy() {
		super.destroy();
		this.socketMono.doOnNext(ZMQ.Socket::close).block();
		this.consumerScheduler.dispose();
	}

}
