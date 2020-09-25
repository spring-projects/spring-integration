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

package org.springframework.integration.zeromq.dsl;

import java.time.Duration;
import java.util.function.Consumer;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.mapping.BytesMessageMapper;
import org.springframework.integration.zeromq.ZeroMqProxy;
import org.springframework.integration.zeromq.channel.ZeroMqChannel;

/**
 * The {@link MessageChannelSpec} for a {@link ZeroMqChannel}.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class ZeroMqChannelSpec extends MessageChannelSpec<ZeroMqChannelSpec, ZeroMqChannel> {

	protected ZeroMqChannelSpec(ZContext context, boolean pubSub) {
		this.channel = new ZeroMqChannel(context, pubSub);
	}

	/**
	 * Configure a connection to the ZeroMQ proxy with the pair of ports over colon
	 * for proxy frontend and backend sockets. Mutually exclusive with the {@link #zeroMqProxy(ZeroMqProxy)}.
	 * @param connectUrl the connection string in format {@code PROTOCOL://HOST:FRONTEND_PORT:BACKEND_PORT},
	 *                    e.g. {@code tcp://localhost:6001:6002}
	 * @return the spec
	 */
	public ZeroMqChannelSpec connectUrl(String connectUrl) {
		this.channel.setConnectUrl(connectUrl);
		return this;
	}

	/**
	 * Specify a reference to a {@link ZeroMqProxy} instance in the same application
	 * to rely on its ports configuration and make a natural lifecycle dependency without guessing
	 * when the proxy is started. Mutually exclusive with the {@link #connectUrl(String)}.
	 * @param zeroMqProxy the {@link ZeroMqProxy} instance to use
	 * @return the spec
	 */
	public ZeroMqChannelSpec zeroMqProxy(ZeroMqProxy zeroMqProxy) {
		this.channel.setZeroMqProxy(zeroMqProxy);
		return this;
	}

	/**
	 * Specify a {@link Duration} to delay consumption when no data received.
	 * @param consumeDelay the {@link Duration} to delay consumption when empty;
	 *                     defaults to {@link ZeroMqChannel#DEFAULT_CONSUME_DELAY}.
	 * @return the spec
	 */
	public ZeroMqChannelSpec consumeDelay(Duration consumeDelay) {
		this.channel.setConsumeDelay(consumeDelay);
		return this;
	}

	/**
	 * Provide a {@link BytesMessageMapper} to convert to/from messages when send or receive happens
	 * on the sockets.
	 * @param messageMapper the {@link BytesMessageMapper} to use.
	 * @return the spec
	 */
	public ZeroMqChannelSpec messageMapper(BytesMessageMapper messageMapper) {
		this.channel.setMessageMapper(messageMapper);
		return this;
	}

	/**
	 * The {@link Consumer} callback to configure a publishing socket.
	 * @param sendSocketConfigurer the {@link Consumer} to use.
	 * @return the spec
	 */
	public ZeroMqChannelSpec sendSocketConfigurer(Consumer<ZMQ.Socket> sendSocketConfigurer) {
		this.channel.setSendSocketConfigurer(sendSocketConfigurer);
		return this;
	}

	/**
	 * The {@link Consumer} callback to configure a consuming socket.
	 * @param subscribeSocketConfigurer the {@link Consumer} to use.
	 * @return the spec
	 */
	public ZeroMqChannelSpec subscribeSocketConfigurer(Consumer<ZMQ.Socket> subscribeSocketConfigurer) {
		this.channel.setSubscribeSocketConfigurer(subscribeSocketConfigurer);
		return this;
	}

}
