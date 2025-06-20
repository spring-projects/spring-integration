/*
 * Copyright 2022-present the original author or authors.
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

package org.springframework.integration.channel.interceptor;

import io.micrometer.common.lang.Nullable;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.aop.support.AopUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * The {@link org.springframework.messaging.support.ExecutorChannelInterceptor}
 * implementation responsible for an {@link Observation} propagation from one message
 * flow's thread to another through the {@link MessageChannel}s involved in the flow.
 * Opens a new {@link Observation.Scope} on another thread and cleans up it in the end.
 * <p>
 * NOTE: This interceptor is proven to be wrong since an existing observation usually is closed
 * on the sender side before the message is consumed on the receiver side.
 * Therefore, it is better to have a {@code sender} observation on this channel,
 * and then {@code receiver} observation on a subscriber for this channel.
 * This way a tracing information is stored into message headers passing this channel.
 * Such an approach also eliminate a problem with persistent message channels where
 * an {@link Observation} is not serializable to be stored into database as a part of the message.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @deprecated since 6.1.7 for removal in 6.4 in favor of enabling observation on the channel and its consumer.
 */
@Deprecated(since = "6.1.7", forRemoval = true)
public class ObservationPropagationChannelInterceptor extends ThreadStatePropagationChannelInterceptor<Observation> {

	private final ThreadLocal<Observation.Scope> scopes = new ThreadLocal<>();

	private final ObservationRegistry observationRegistry;

	public ObservationPropagationChannelInterceptor(ObservationRegistry observationRegistry) {
		Assert.notNull(observationRegistry, "'observationRegistry' must noty be null");
		this.observationRegistry = observationRegistry;
	}

	@Override
	@Nullable
	protected Observation obtainPropagatingContext(Message<?> message, MessageChannel channel) {
		if (!DirectChannel.class.isAssignableFrom(AopUtils.getTargetClass(channel))) {
			return this.observationRegistry.getCurrentObservation();
		}
		return null;
	}

	@Override
	protected void populatePropagatedContext(@Nullable Observation state, Message<?> message, MessageChannel channel) {
		if (state != null) {
			Observation.Scope scope = state.openScope();
			this.scopes.set(scope);
		}
	}

	@Override
	public void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex) {
		Observation.Scope scope = this.scopes.get();
		if (scope != null && scope.equals(this.observationRegistry.getCurrentObservationScope())) {
			scope.close();
			this.scopes.remove();
		}
	}

}
