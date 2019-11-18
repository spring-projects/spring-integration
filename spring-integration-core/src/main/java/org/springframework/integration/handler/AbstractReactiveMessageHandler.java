/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.handler;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.management.metrics.SampleFacade;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;

/**
 * Base class for {@link ReactiveMessageHandler} implementations.
 *
 * @author David Turanski
 *
 * @since 5.3
 */
public abstract class AbstractReactiveMessageHandler extends AbstractBaseMessageHandler
		implements ReactiveMessageHandler {

	@Override
	public Mono<Void> handleMessage(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(message.getPayload(), "Message payload must not be null"); // NOSONAR - false positive
		if (this.loggingEnabled && this.logger.isDebugEnabled()) {
			this.logger.debug(this + " received message: " + message);
		}
		//TODO: Can't capture metrics the same way in this context since returning a Mono<>
		org.springframework.integration.support.management.MetricsContext start = null;

		SampleFacade sample = null;
		if (this.countsEnabled && this.metricsCaptor != null) {
			sample = this.metricsCaptor.start();
		}
		try {
			if (this.shouldTrack) {
				message = MessageHistory.write(message, this, getMessageBuilderFactory());
			}
			return handleMessageInternal(message);
		}
		catch (Exception e) {
			if (sample != null) {
				sample.stop(buildSendTimer(false, e.getClass().getSimpleName()));
			}
			if (this.countsEnabled) {
				this.handlerMetrics.afterHandle(start, false);
			}
			throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
					() -> "error occurred in message handler [" + this + "]", e);
		}
	}

	protected abstract Mono<Void> handleMessageInternal(Message<?> message);
}
