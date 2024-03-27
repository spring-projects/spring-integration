/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.jms.dsl;

import java.util.function.Consumer;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsInboundGateway;
import org.springframework.integration.util.CheckedFunction;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * A {@link MessagingGatewaySpec} for a {@link JmsInboundGateway}.
 *
 * @param <S> the target {@link JmsInboundGatewaySpec} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class JmsInboundGatewaySpec<S extends JmsInboundGatewaySpec<S>>
		extends MessagingGatewaySpec<S, JmsInboundGateway> {

	protected JmsInboundGatewaySpec(AbstractMessageListenerContainer listenerContainer) {
		super(new JmsInboundGateway(listenerContainer, new ChannelPublishingJmsMessageListener()));
		this.target.getListener().setExpectReply(true);
	}

	/**
	 * @param defaultReplyDestination the defaultReplyDestination
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDefaultReplyDestination(Destination)
	 */
	public S defaultReplyDestination(Destination defaultReplyDestination) {
		this.target.getListener().setDefaultReplyDestination(defaultReplyDestination);
		return _this();
	}

	/**
	 * @param destinationName the destinationName
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDefaultReplyQueueName(String)
	 */
	public S defaultReplyQueueName(String destinationName) {
		this.target.getListener().setDefaultReplyQueueName(destinationName);
		return _this();
	}

	/**
	 * @param destinationName the destinationName
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDefaultReplyTopicName(String)
	 */
	public S defaultReplyTopicName(String destinationName) {
		this.target.getListener().setDefaultReplyTopicName(destinationName);
		return _this();
	}

	/**
	 * @param replyTimeToLive the replyTimeToLive
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setReplyTimeToLive(long)
	 */
	public S replyTimeToLive(long replyTimeToLive) {
		this.target.getListener().setReplyTimeToLive(replyTimeToLive);
		return _this();
	}

	/**
	 * @param replyPriority the replyPriority
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setReplyPriority(int)
	 */
	public S replyPriority(int replyPriority) {
		this.target.getListener().setReplyPriority(replyPriority);
		return _this();
	}

	/**
	 * @param replyDeliveryPersistent the replyDeliveryPersistent
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setReplyDeliveryPersistent(boolean)
	 */
	public S replyDeliveryPersistent(boolean replyDeliveryPersistent) {
		this.target.getListener().setReplyDeliveryPersistent(replyDeliveryPersistent);
		return _this();
	}

	/**
	 * @param correlationKey the correlationKey
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setCorrelationKey(String)
	 */
	public S correlationKey(String correlationKey) {
		this.target.getListener().setCorrelationKey(correlationKey);
		return _this();
	}

	/**
	 * @param explicitQosEnabledForReplies the explicitQosEnabledForReplies.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExplicitQosEnabledForReplies(boolean)
	 */
	public S explicitQosEnabledForReplies(boolean explicitQosEnabledForReplies) {
		this.target.getListener().setExplicitQosEnabledForReplies(explicitQosEnabledForReplies);
		return _this();
	}

	/**
	 * @param destinationResolver the destinationResolver.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setDestinationResolver(DestinationResolver)
	 */
	public S destinationResolver(DestinationResolver destinationResolver) {
		this.target.getListener().setDestinationResolver(destinationResolver);
		return _this();
	}

	/**
	 * Set a SpEL expression to resolve a 'replyTo' destination from a request {@link jakarta.jms.Message}
	 * as a root evaluation object if {@link jakarta.jms.Message#getJMSReplyTo()} is null.
	 * @param replyToExpression the SpEL expression for 'replyTo' destination.
	 * @return the spec.
	 * @since 6.1
	 * @see ChannelPublishingJmsMessageListener#setReplyToExpression(Expression)
	 */
	public S replyToExpression(String replyToExpression) {
		return replyToExpression(PARSER.parseExpression(replyToExpression));
	}

	/**
	 * Set a function to resolve a 'replyTo' destination from a request {@link jakarta.jms.Message}
	 * as a root evaluation object if {@link jakarta.jms.Message#getJMSReplyTo()} is null.
	 * @param replyToFunction the function for 'replyTo' destination.
	 * @return the spec.
	 * @since 6.1
	 * @see ChannelPublishingJmsMessageListener#setReplyToExpression(Expression)
	 */
	public S replyToFunction(CheckedFunction<Message, ?, JMSException> replyToFunction) {
		return replyToExpression(new FunctionExpression<>(replyToFunction.unchecked()));
	}

	/**
	 * Set a SpEL expression to resolve a 'replyTo' destination from a request {@link jakarta.jms.Message}
	 * as a root evaluation object if {@link jakarta.jms.Message#getJMSReplyTo()} is null.
	 * @param replyToExpression the SpEL expression for 'replyTo' destination.
	 * @return the spec.
	 * @since 6.1
	 * @see ChannelPublishingJmsMessageListener#setReplyToExpression(Expression)
	 */
	public S replyToExpression(Expression replyToExpression) {
		this.target.getListener().setReplyToExpression(replyToExpression);
		return _this();
	}

	/**
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setMessageConverter(MessageConverter)
	 */
	public S jmsMessageConverter(MessageConverter messageConverter) {
		this.target.getListener().setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setHeaderMapper(JmsHeaderMapper)
	 */
	public S setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.target.getListener().setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * @param extractRequestPayload the extractRequestPayload.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExtractRequestPayload(boolean)
	 */
	public S extractRequestPayload(boolean extractRequestPayload) {
		this.target.getListener().setExtractRequestPayload(extractRequestPayload);
		return _this();
	}

	/**
	 * @param extractReplyPayload the extractReplyPayload.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExtractReplyPayload(boolean)
	 */
	public S extractReplyPayload(boolean extractReplyPayload) {
		this.target.getListener().setExtractReplyPayload(extractReplyPayload);
		return _this();
	}

	/**
	 * Set a {@link RetryTemplate} to use for retrying a message delivery within the
	 * adapter. Unlike adding retry at the container level, this can be used with an
	 * {@code ErrorMessageSendingRecoverer} {@link RecoveryCallback} to publish to the
	 * error channel after retries are exhausted. You generally should not configure an
	 * error channel when using retry here, use a {@link RecoveryCallback} instead.
	 * @param retryTemplate the template.
	 * @since 6.3
	 * @see #recoveryCallback(RecoveryCallback)
	 */
	public S retryTemplate(RetryTemplate retryTemplate) {
		this.target.getListener().setRetryTemplate(retryTemplate);
		return _this();
	}

	/**
	 * Set a {@link RecoveryCallback} when using retry within the adapter.
	 * @param recoveryCallback the callback.
	 * @since 6.3
	 * @see #retryTemplate(RetryTemplate)
	 */
	public S recoveryCallback(RecoveryCallback<org.springframework.messaging.Message<?>> recoveryCallback) {
		this.target.getListener().setRecoveryCallback(recoveryCallback);
		return _this();
	}

	/**
	 * Set to {@code false} to prevent listener container shutdown when the endpoint is stopped.
	 * Then, if so configured, any cached consumer(s) in the container will remain.
	 * Otherwise, the shared connection and will be closed and the listener invokers shut
	 * down; this behavior is new starting with version 5.1. Default: true.
	 * @param shutdown false to not shutdown.
	 * @return the spec.
	 * @since 5.1
	 */
	public S shutdownContainerOnStop(boolean shutdown) {
		this.target.setShutdownContainerOnStop(shutdown);
		return _this();
	}

	/**
	 * An {@link AbstractMessageListenerContainer}-based {@link JmsInboundGatewaySpec} extension.
	 *
	 * @param <S> the target {@link JmsListenerContainerSpec} implementation type.
	 * @param <C> the target {@link AbstractMessageListenerContainer} implementation type.
	 */
	public static class JmsInboundGatewayListenerContainerSpec<S extends JmsListenerContainerSpec<S, C>,
			C extends AbstractMessageListenerContainer>
			extends JmsInboundGatewaySpec<JmsInboundGatewayListenerContainerSpec<S, C>> {

		private final S spec;

		protected JmsInboundGatewayListenerContainerSpec(S spec) {
			super(spec.getObject());
			this.spec = spec;
			this.spec.getObject().setAutoStartup(false);
		}

		/**
		 * Specify a request destination for incoming messages.
		 * @param requestDestination the destination
		 * @return the spec.
		 * @since 5.5
		 * @see JmsListenerContainerSpec#destination(Destination)
		 */
		public JmsInboundGatewayListenerContainerSpec<S, C> requestDestination(Destination requestDestination) {
			this.spec.destination(requestDestination);
			return _this();
		}

		/**
		 * Specify a request destination for incoming messages.
		 * @param requestDestinationName the destination name
		 * @return the spec.
		 * @since 5.5
		 * @see JmsListenerContainerSpec#destination(String)
		 */
		public JmsInboundGatewayListenerContainerSpec<S, C> requestDestination(String requestDestinationName) {
			this.spec.destination(requestDestinationName);
			return _this();
		}

		/**
		 * Specify a {@link Consumer} to accept a {@link JmsListenerContainerSpec} for further configuration.
		 * @param configurer the {@link Consumer} to accept a {@link JmsListenerContainerSpec}
		 *                         for further configuration.
		 * @return the spec
		 */
		public JmsInboundGatewayListenerContainerSpec<S, C> configureListenerContainer(Consumer<S> configurer) {
			Assert.notNull(configurer, "'configurer' must not be null");
			configurer.accept(this.spec);
			return _this();
		}

	}

}
