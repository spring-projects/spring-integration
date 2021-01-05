/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.metrics.CounterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.management.metrics.TimerFacade;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * A channel implementation that essentially behaves like "/dev/null".
 * All {@link #receive()} calls will return {@code null},
 * and all {@link #send} calls will return {@code true} although no action is performed.
 * Unless the payload of a sent message is a {@link Publisher} implementation, in
 * which case the {@link Publisher#subscribe(Subscriber)} is called to initiate
 * the reactive stream, although the data is discarded by this channel.
 * An error thrown from a reactive stream processing (see {@link Subscriber#onError(Throwable)})
 * is logged under the {@code warn} level.
 * Note however that the invocations are logged at debug-level.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@IntegrationManagedResource
public class NullChannel implements PollableChannel,
		BeanNameAware, IntegrationManagement, IntegrationPattern {

	private static final LogAccessor LOG = new LogAccessor(NullChannel.class);

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	private boolean loggingEnabled = true;

	private String beanName;

	private MetricsCaptor metricsCaptor;

	private TimerFacade successTimer;

	private CounterFacade receiveCounter;

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	@Override
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
		this.managementOverrides.loggingConfigured = true;
	}

	@Override
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	@Nullable
	public String getComponentName() {
		return this.beanName;
	}

	@Override
	public String getComponentType() {
		return "null-channel";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.null_channel;
	}

	@Override
	public void registerMetricsCaptor(MetricsCaptor registry) {
		this.metricsCaptor = registry;
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.managementOverrides;
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		return send(message);
	}

	@Override
	public boolean send(Message<?> message) {
		if (this.loggingEnabled) {
			LOG.debug(() -> "message sent to null channel: " + message);
		}

		Object payload = message.getPayload();
		if (payload instanceof Publisher<?>) {
			((Publisher<?>) payload).subscribe(
					new Subscriber<Object>() {

						@Override public void onSubscribe(Subscription subscription) {
							subscription.request(Long.MAX_VALUE);
						}

						@Override public void onNext(Object value) {

						}

						@Override public void onError(Throwable ex) {
							LOG.warn(ex, "An error happened in a reactive stream processing");
						}

						@Override public void onComplete() {

						}

					});
		}

		if (this.metricsCaptor != null) {
			sendTimer().record(0, TimeUnit.MILLISECONDS);
		}
		return true;
	}

	private TimerFacade sendTimer() {
		if (this.successTimer == null) {
			this.successTimer =
					this.metricsCaptor.timerBuilder(SEND_TIMER_NAME)
							.tag("type", "channel")
							.tag("name", getComponentName() == null ? "nullChannel" : getComponentName())
							.tag("result", "success")
							.tag("exception", "none")
							.description("Subflow process time")
							.build();
		}
		return this.successTimer;
	}

	@Override
	public Message<?> receive() {
		if (this.loggingEnabled) {
			LOG.debug("receive called on null channel");
		}
		incrementReceiveCounter();
		return null;
	}

	@Override
	public Message<?> receive(long timeout) {
		return receive();
	}

	private void incrementReceiveCounter() {
		if (this.metricsCaptor != null) {
			if (this.receiveCounter == null) {
				this.receiveCounter = buildReceiveCounter();
			}
			this.receiveCounter.increment();
		}
	}

	private CounterFacade buildReceiveCounter() {
		return this.metricsCaptor
				.counterBuilder(RECEIVE_COUNTER_NAME)
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("type", "channel")
				.tag("result", "success")
				.tag("exception", "none")
				.description("Messages received")
				.build();
	}

	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

	@Override
	public void destroy() {
		if (this.successTimer != null) {
			this.successTimer.remove();
		}
		if (this.receiveCounter != null) {
			this.receiveCounter.remove();
		}
	}

}
