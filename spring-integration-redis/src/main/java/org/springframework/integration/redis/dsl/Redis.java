/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.redis.dsl;

import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Factory class for Redis components.
 *
 * @author Jiandong Ma
 *
 * @since 7.1
 */
public final class Redis {

	/**
	 * The factory to produce a {@link RedisInboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisInboundChannelAdapterSpec} instance
	 */
	public static RedisInboundChannelAdapterSpec inboundChannelAdapter(RedisConnectionFactory connectionFactory) {
		return new RedisInboundChannelAdapterSpec(connectionFactory);
	}

	/**
	 * The factory to produce a {@link RedisOutboundChannelAdapterSpec}.
	 * @param connectionFactory the {@link RedisConnectionFactory} to build on
	 * @return the {@link RedisOutboundChannelAdapterSpec} instance
	 */
	public static RedisOutboundChannelAdapterSpec outboundChannelAdapter(RedisConnectionFactory connectionFactory) {
		return new RedisOutboundChannelAdapterSpec(connectionFactory);
	}

	private Redis() {
	}

}
