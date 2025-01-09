/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.acks.AckUtils;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.context.ExpressionCapable;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.integration.support.management.observation.DefaultMessageReceiverObservationConvention;
import org.springframework.integration.support.management.observation.IntegrationObservation;
import org.springframework.integration.support.management.observation.MessageReceiverContext;
import org.springframework.integration.support.management.observation.MessageReceiverObservationConvention;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A Channel Adapter implementation for connecting a {@link MessageSource} to a {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class SourcePollingChannelAdapter extends AbstractPollingEndpoint
		implements TrackableComponent, IntegrationManagement {

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private MessageSource<?> originalSource;

	private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

	private MessageReceiverObservationConvention observationConvention;

	private volatile MessageSource<?> source;

	private volatile MessageChannel outputChannel;

	private volatile String outputChannelName;

	private volatile boolean shouldTrack;

	private final Lock lock = new ReentrantLock();

	/**
	 * Specify the source to be polled for Messages.
	 * @param source The message source.
	 */
	public void setSource(MessageSource<?> source) {
		this.source = source;

		Object target = extractProxyTarget(source);
		this.originalSource = target != null ? (MessageSource<?>) target : source;

		if (source instanceof ExpressionCapable expressionCapable) {
			setPrimaryExpression(expressionCapable.getExpression());
		}
	}

	/**
	 * Specify the {@link MessageChannel} where Messages should be sent.
	 * @param outputChannel The output channel.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Return this endpoint's source.
	 * @return the source.
	 * @since 4.3
	 */
	public MessageSource<?> getMessageSource() {
		return this.source;
	}

	public void setOutputChannelName(String outputChannelName) {
		Assert.hasText(outputChannelName, "'outputChannelName' must not be empty");
		this.outputChannelName = outputChannelName;
	}

	/**
	 * Specify the maximum time to wait for a Message to be sent to the output channel.
	 * @param sendTimeout The send timeout.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Specify whether this component should be tracked in the Message History.
	 * @param shouldTrack true if the component should be tracked.
	 */
	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	@Override
	public void registerObservationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Set a custom {@link MessageReceiverObservationConvention} for {@link IntegrationObservation#HANDLER}.
	 * Ignored if an {@link ObservationRegistry} is not configured for this component.
	 * @param observationConvention the {@link MessageReceiverObservationConvention} to use.
	 * @since 6.5
	 */
	public void setObservationConvention(@Nullable MessageReceiverObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

	@Override
	public boolean isObserved() {
		return !ObservationRegistry.NOOP.equals(this.observationRegistry);
	}

	@Override
	public String getComponentType() {
		return (this.source instanceof NamedComponent namedComponent)
				? namedComponent.getComponentType()
				: "inbound-channel-adapter";
	}

	@Override
	protected boolean isReactive() {
		return getOutputChannel() instanceof ReactiveStreamsSubscribableChannel;
	}

	@Override
	protected Object getReceiveMessageSource() {
		return getMessageSource();
	}

	@Override
	protected final void setReceiveMessageSource(Object source) {
		this.source = (MessageSource<?>) source;
	}

	@Override
	protected void doStart() {
		if (this.source instanceof Lifecycle lifecycle) {
			lifecycle.start();
		}
		super.doStart();

		if (isReactive()) {
			((ReactiveStreamsSubscribableChannel) this.outputChannel).subscribeTo(getPollingFlux());
		}
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (this.source instanceof Lifecycle lifecycle) {
			lifecycle.stop();
		}
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.source, "source must not be null");
		Assert.state((this.outputChannelName == null && this.outputChannel != null)
						|| (this.outputChannelName != null && this.outputChannel == null),
				"One and only one of 'outputChannelName' or 'outputChannel' is required.");
		super.onInit();
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
		}
	}

	public MessageChannel getOutputChannel() {
		if (this.outputChannelName != null) {
			this.lock.lock();
			try {
				if (this.outputChannelName != null) {
					this.outputChannel = getChannelResolver().resolveDestination(this.outputChannelName);
					this.outputChannelName = null;
				}
			}
			finally {
				this.lock.unlock();
			}
		}
		return this.outputChannel;
	}

	@Override
	protected void handleMessage(Message<?> messageArg) {
		Message<?> message = messageArg;
		if (this.shouldTrack) {
			message = MessageHistory.write(message, this, getMessageBuilderFactory());
		}
		AcknowledgmentCallback ackCallback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(message);
		try {
			this.messagingTemplate.send(getOutputChannel(), message);
			AckUtils.autoAck(ackCallback);
		}
		catch (Exception ex) {
			AckUtils.autoNack(ackCallback);
			if (ex instanceof MessagingException messagingException) { // NOSONAR
				throw messagingException;
			}
			else {
				throw new MessagingException(message, "Failed to send Message", ex);
			}
		}
	}

	@Override
	protected Message<?> receiveMessage() {
		return this.source.receive();
	}

	/**
	 * Start an observation (and open scope) for the received message.
	 * @param holder the resource holder for this component.
	 * @param message the received message.
	 */
	@Override
	protected void messageReceived(@Nullable IntegrationResourceHolder holder, Message<?> message) {
		Observation observation =
				IntegrationObservation.HANDLER.observation(this.observationConvention,
						DefaultMessageReceiverObservationConvention.INSTANCE,
						() -> new MessageReceiverContext(message, getComponentName(), "message-source"),
						this.observationRegistry);

		observation.start().openScope();
		super.messageReceived(holder, message);
	}

	/**
	 * Stop an observation (and close its scope) previously started
	 * from the {@link #messageReceived(IntegrationResourceHolder, Message)}.
	 * @param pollingTaskError an optional error as a result of the polling task.
	 */
	@Override
	protected void donePollingTask(@Nullable Exception pollingTaskError) {
		Observation.Scope currentObservationScope = this.observationRegistry.getCurrentObservationScope();
		if (currentObservationScope != null) {
			currentObservationScope.close();
			Observation currentObservation = currentObservationScope.getCurrentObservation();
			if (pollingTaskError != null) {
				currentObservation.error(pollingTaskError);
			}
			currentObservation.stop();
		}
	}

	@Override
	protected Object getResourceToBind() {
		return this.originalSource;
	}

	@Override
	protected String getResourceKey() {
		return IntegrationResourceHolder.MESSAGE_SOURCE;
	}

	@Nullable
	private static Object extractProxyTarget(Object target) {
		if (!(target instanceof Advised advised)) {
			return target;
		}
		try {
			return extractProxyTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception ex) {
			throw new BeanCreationException("Could not extract target", ex);
		}
	}

}
