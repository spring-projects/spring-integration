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

import org.zeromq.SocketType;
import org.zeromq.ZContext;

/**
 * Factory class for ZeroMq components DSL.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public final class ZeroMq {

	/**
	 * Create an instance of {@link ZeroMqChannelSpec} based on the provided {@link ZContext}.
	 * @param context the {@link ZContext} to use.
	 * @return the spec.
	 */
	public static ZeroMqChannelSpec zeroMqChannel(ZContext context) {
		return new ZeroMqChannelSpec(context, false);
	}

	/**
	 * Create an instance of {@link ZeroMqChannelSpec} in pub/sub mode based on the provided {@link ZContext}.
	 * @param context the {@link ZContext} to use.
	 * @return the spec.
	 */
	public static ZeroMqChannelSpec pubSubZeroMqChannel(ZContext context) {
		return new ZeroMqChannelSpec(context, true);
	}

	/**
	 * Create an instance of {@link ZeroMqMessageHandlerSpec} for the provided {@link ZContext} and connection URL.
	 * @param context the {@link ZContext} to use.
	 * @param connectUrl the URL to connect a ZeroMq socket to.
	 * @return the spec.
	 */
	public static ZeroMqMessageHandlerSpec outboundChannelAdapter(ZContext context, String connectUrl) {
		return new ZeroMqMessageHandlerSpec(context, connectUrl);
	}

	/**
	 * Create an instance of {@link ZeroMqMessageHandlerSpec} for the provided {@link ZContext}, connection URL
	 * and {@link SocketType}.
	 * @param context the {@link ZContext} to use.
	 * @param connectUrl the URL to connect a ZeroMq socket to.
	 * @param socketType the {@link SocketType} for ZeroMq socket.
	 * @return the spec.
	 */
	public static ZeroMqMessageHandlerSpec outboundChannelAdapter(ZContext context, String connectUrl,
			SocketType socketType) {

		return new ZeroMqMessageHandlerSpec(context, connectUrl, socketType);
	}

	/**
	 * Create an instance of {@link ZeroMqMessageProducerSpec} for the provided {@link ZContext}.
	 * @param context the {@link ZContext} to use.
	 * @return the spec.
	 */
	public static ZeroMqMessageProducerSpec inboundChannelAdapter(ZContext context) {
		return new ZeroMqMessageProducerSpec(context);
	}

	/**
	 * Create an instance of {@link ZeroMqMessageProducerSpec} for the provided {@link ZContext}
	 * and {@link SocketType}.
	 * @param context the {@link ZContext} to use.
	 * @param socketType the {@link SocketType} for ZeroMq socket.
	 * @return the spec.
	 */
	public static ZeroMqMessageProducerSpec inboundChannelAdapter(ZContext context, SocketType socketType) {
		return new ZeroMqMessageProducerSpec(context, socketType);
	}


	private ZeroMq() {
	}

}
