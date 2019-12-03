/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.channel;

import org.reactivestreams.Publisher;

import org.springframework.messaging.Message;

import reactor.core.publisher.Mono;

/**
 * Reactive Streams specific message channel for composing upstream {@link Publisher}s
 * into an on-demand processing in this channel implementation.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public interface ReactiveStreamsSubscribableChannel {

	/**
	 * Subscribe to the provided {@link Publisher}.
	 * @param publisher the {@link Publisher} to subscribe to.
	 * @deprecated in favor of {@link #subscribeToUpstream}.
	 * Framework doesn't call this method internally any more.
	 */
	@Deprecated
	void subscribeTo(Publisher<? extends Message<?>> publisher);

	/**
	 * Compose with the provided upstream {@link Publisher} for processing its events in this channel.
	 * It is recommended to implement this method for possible reactive streams composition.
	 * @param upstreamPublisher the upstream {@link Publisher} to subscribe to.
	 * @return the {@link Mono} for reactive streams composition.
	 * @since 5.2.2
	 */
	@SuppressWarnings("deprecagtion")
	default Mono<Void> subscribeToUpstream(Publisher<? extends Message<?>> upstreamPublisher) {
		subscribeTo(upstreamPublisher);
		return Mono.empty();
	}

}
