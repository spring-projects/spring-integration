/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.Ordered;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.management.AbstractMessageHandlerMetrics;
import org.springframework.integration.support.management.ConfigurableMetricsAware;
import org.springframework.integration.support.management.DefaultMessageHandlerMetrics;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.MessageHandlerMetrics;
import org.springframework.integration.support.management.MetricsContext;
import org.springframework.integration.support.management.Statistics;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

import reactor.core.Exceptions;

/**
 * Base class for MessageHandler implementations that provides basic validation
 * and error handling capabilities. Asserts that the incoming Message is not
 * null and that it does not contain a null payload. Converts checked exceptions
 * into runtime {@link MessagingException}s.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
@IntegrationManagedResource
public abstract class AbstractMessageHandler extends IntegrationObjectSupport implements MessageHandler,
		MessageHandlerMetrics, ConfigurableMetricsAware<AbstractMessageHandlerMetrics>, TrackableComponent, Orderable,
		Subscriber<Message<?>> {

	private volatile boolean shouldTrack = false;

	private volatile int order = Ordered.LOWEST_PRECEDENCE;

	private volatile AbstractMessageHandlerMetrics handlerMetrics = new DefaultMessageHandlerMetrics();

	private volatile boolean statsEnabled;

	private volatile boolean countsEnabled;

	private volatile String managedName;

	private volatile String managedType;

	private volatile boolean loggingEnabled = true;

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	@Override
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}

	@Override
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public String getComponentType() {
		return "message-handler";
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	public void configureMetrics(AbstractMessageHandlerMetrics metrics) {
		Assert.notNull(metrics, "'metrics' must not be null");
		this.handlerMetrics = metrics;
	}

	@Override
	protected void onInit() throws Exception {
		if (this.statsEnabled) {
			this.handlerMetrics.setFullStatsEnabled(true);
		}
	}

	@Override
	public final void handleMessage(Message<?> message) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(message.getPayload(), "Message payload must not be null"); //NOSONAR - false positive
		if (this.loggingEnabled && this.logger.isDebugEnabled()) {
			this.logger.debug(this + " received message: " + message);
		}
		MetricsContext start = null;
		boolean countsEnabled = this.countsEnabled;
		AbstractMessageHandlerMetrics handlerMetrics = this.handlerMetrics;
		try {
			if (this.shouldTrack) {
				message = MessageHistory.write(message, this, this.getMessageBuilderFactory());
			}
			if (countsEnabled) {
				start = handlerMetrics.beforeHandle();
			}
			this.handleMessageInternal(message);
			if (countsEnabled) {
				handlerMetrics.afterHandle(start, true);
			}
		}
		catch (Exception e) {
			if (countsEnabled) {
				handlerMetrics.afterHandle(start, false);
			}
			if (e instanceof MessagingException) {
				throw (MessagingException) e;
			}
			throw new MessageHandlingException(message, "error occurred in message handler [" + this + "]", e);
		}
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		Assert.notNull(subscription, "'subscription' must not be null");
		subscription.request(Long.MAX_VALUE);
	}

	@Override
	public void onNext(Message<?> message) {
		handleMessage(message);
	}

	@Override
	public void onError(Throwable throwable) {
		Exceptions.throwIfFatal(throwable);
		if (throwable instanceof MessagingException) {
			throw (MessagingException) throwable;
		}
		throw new MessagingException("Error occurred in message handler [" + this + "]", throwable);
	}

	@Override
	public void onComplete() {

	}

	protected abstract void handleMessageInternal(Message<?> message) throws Exception;

	@Override
	public void reset() {
		this.handlerMetrics.reset();
	}

	@Override
	public long getHandleCountLong() {
		return this.handlerMetrics.getHandleCountLong();
	}

	@Override
	public int getHandleCount() {
		return this.handlerMetrics.getHandleCount();
	}

	@Override
	public int getErrorCount() {
		return this.handlerMetrics.getErrorCount();
	}

	@Override
	public long getErrorCountLong() {
		return this.handlerMetrics.getErrorCountLong();
	}

	@Override
	public double getMeanDuration() {
		return this.handlerMetrics.getMeanDuration();
	}

	@Override
	public double getMinDuration() {
		return this.handlerMetrics.getMinDuration();
	}

	@Override
	public double getMaxDuration() {
		return this.handlerMetrics.getMaxDuration();
	}

	@Override
	public double getStandardDeviationDuration() {
		return this.handlerMetrics.getStandardDeviationDuration();
	}

	@Override
	public int getActiveCount() {
		return this.handlerMetrics.getActiveCount();
	}

	@Override
	public long getActiveCountLong() {
		return this.handlerMetrics.getActiveCountLong();
	}

	@Override
	public Statistics getDuration() {
		return this.handlerMetrics.getDuration();
	}

	@Override
	public void setStatsEnabled(boolean statsEnabled) {
		if (statsEnabled) {
			this.countsEnabled = true;
		}
		this.statsEnabled = statsEnabled;
		if (this.handlerMetrics != null) {
			this.handlerMetrics.setFullStatsEnabled(statsEnabled);
		}
	}

	@Override
	public boolean isStatsEnabled() {
		return this.statsEnabled;
	}

	@Override
	public void setCountsEnabled(boolean countsEnabled) {
		this.countsEnabled = countsEnabled;
		if (!countsEnabled) {
			this.statsEnabled = false;
		}
	}

	@Override
	public boolean isCountsEnabled() {
		return this.countsEnabled;
	}

	@Override
	public void setManagedName(String managedName) {
		this.managedName = managedName;
	}

	@Override
	public String getManagedName() {
		return this.managedName;
	}

	@Override
	public void setManagedType(String managedType) {
		this.managedType = managedType;
	}

	@Override
	public String getManagedType() {
		return this.managedType;
	}

}
