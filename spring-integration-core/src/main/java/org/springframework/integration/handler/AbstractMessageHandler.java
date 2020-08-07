/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.integration.support.management.metrics.MetricsCaptor;
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
 * @author Artem Bilan
 */
public abstract class AbstractMessageHandler extends MessageHandlerSupport
		implements MessageHandler, CoreSubscriber<Message<?>> {

	@Override // NOSONAR
	public void handleMessage(Message<?> message) {
		Message<?> messageToUse = message;
		Assert.notNull(messageToUse, "Message must not be null");
		if (isLoggingEnabled() && this.logger.isDebugEnabled()) {
			this.logger.debug(this + " received message: " + messageToUse);
		}
		SampleFacade sample = null;
		MetricsCaptor metricsCaptor = getMetricsCaptor();
		if (metricsCaptor != null) {
			sample = metricsCaptor.start();
		}
		try {
			if (shouldTrack()) {
				messageToUse = MessageHistory.write(messageToUse, this, getMessageBuilderFactory());
			}
			handleMessageInternal(messageToUse);
			if (sample != null) {
				sample.stop(sendTimer());
			}
		}
		catch (Exception e) {
			if (sample != null) {
				sample.stop(buildSendTimer(false, e.getClass().getSimpleName()));
			}
			throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(messageToUse,
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
