/*
 * Copyright 2002-2019 the original author or authors.
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

import org.reactivestreams.Subscription;

import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.management.metrics.SampleFacade;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

import reactor.core.CoreSubscriber;

/**
 * Base class for {@link MessageHandler} implementations.
 *
 * @author David Turanski
 */
public abstract class AbstractMessageHandler extends AbstractBaseMessageHandler
		implements MessageHandler, CoreSubscriber<Message<?>> {
	public void handleMessage(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		if (this.loggingEnabled && this.logger.isDebugEnabled()) {
			this.logger.debug(this + " received message: " + message);
		}
		org.springframework.integration.support.management.MetricsContext start = null;
		SampleFacade sample = null;
		if (this.countsEnabled && this.metricsCaptor != null) {
			sample = this.metricsCaptor.start();
		}
		try {
			if (this.shouldTrack) {
				message = MessageHistory.write(message, this, getMessageBuilderFactory());
			}
			if (this.countsEnabled) {
				start = this.handlerMetrics.beforeHandle();
				handleMessageInternal(message);
				if (sample != null) {
					sample.stop(sendTimer());
				}
				this.handlerMetrics.afterHandle(start, true);
			}
			else {
				handleMessageInternal(message);
			}
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

	@Override
	public void onSubscribe(Subscription subscription) {
		Assert.notNull(subscription, "'subscription' must not be null");
		subscription.request(Long.MAX_VALUE);
	}

	@Override
	public void onError(Throwable throwable) {

	}

	@Override
	public void onComplete() {

	}

	@Override
	public void onNext(Message<?> message) {
		handleMessage(message);
	}

	protected abstract void handleMessageInternal(Message<?> message);
}
