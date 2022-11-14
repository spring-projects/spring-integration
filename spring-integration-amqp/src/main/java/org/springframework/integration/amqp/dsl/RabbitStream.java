/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import com.rabbitmq.stream.Codec;
import com.rabbitmq.stream.Environment;

import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;

/**
 * Factory class for RabbitMQ components.
 *
 * @author Gary Russell
 * @since 6.0
 *
 */
public final class RabbitStream {

	private RabbitStream() {
	}

	/**
	 * Create an initial {@link RabbitStreamInboundChannelAdapterSpec}
	 * with the provided {@link StreamListenerContainer}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link StreamListenerContainer} using
	 * {@link RabbitStreamInboundChannelAdapterSpec#configureContainer(java.util.function.Consumer)}.
	 * @param listenerContainer the listenerContainer.
	 * @return the RabbitInboundChannelAdapterSLCSpec.
	 */
	public static RabbitStreamInboundChannelAdapterSpec inboundAdapter(StreamListenerContainer listenerContainer) {
		return new RabbitStreamInboundChannelAdapterSpec(listenerContainer);
	}

	/**
	 * Create an initial {@link RabbitStreamInboundChannelAdapterSpec}
	 * with the provided {@link Environment}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link StreamListenerContainer} using
	 * {@link RabbitStreamInboundChannelAdapterSpec#configureContainer(java.util.function.Consumer)}.
	 * @param environment the environment.
	 * @return the RabbitInboundChannelAdapterSLCSpec.
	 */
	public static RabbitStreamInboundChannelAdapterSpec inboundAdapter(Environment environment) {
		return new RabbitStreamInboundChannelAdapterSpec(environment, null);
	}

	/**
	 * Create an initial {@link RabbitStreamInboundChannelAdapterSpec}
	 * with the provided {@link Environment}.
	 * Note: only endpoint options are available from spec.
	 * The {@code listenerContainer} options should be specified
	 * on the provided {@link StreamListenerContainer} using
	 * {@link RabbitStreamInboundChannelAdapterSpec#configureContainer(java.util.function.Consumer)}.
	 * @param environment the environment.
	 * @param codec the codec.
	 * @return the RabbitInboundChannelAdapterSLCSpec.
	 */
	public static RabbitStreamInboundChannelAdapterSpec inboundAdapter(Environment environment, Codec codec) {
		return new RabbitStreamInboundChannelAdapterSpec(environment, codec);
	}

	/**
	 * Create an initial {@link RabbitStreamMessageHandlerSpec} (adapter).
	 * @param template the amqpTemplate.
	 * @return the RabbitStreamMessageHandlerSpec.
	 */
	public static RabbitStreamMessageHandlerSpec outboundStreamAdapter(RabbitStreamTemplate template) {
		return new RabbitStreamMessageHandlerSpec(template);
	}

}
