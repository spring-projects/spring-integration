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

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;

import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for a {@link JmsOutboundGateway}.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class JmsOutboundGatewaySpec extends MessageHandlerSpec<JmsOutboundGatewaySpec, JmsOutboundGateway> {

	protected JmsOutboundGatewaySpec(ConnectionFactory connectionFactory) {
		this.target = new JmsOutboundGateway();
		this.target.setConnectionFactory(connectionFactory);
		this.target.setRequiresReply(true);
	}

	/**
	 * @param extractPayload the extractPayload.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setExtractRequestPayload(boolean)
	 */
	public JmsOutboundGatewaySpec extractRequestPayload(boolean extractPayload) {
		this.target.setExtractRequestPayload(extractPayload);
		return _this();
	}

	/**
	 * @param extractPayload the extractPayload.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setExtractReplyPayload(boolean)
	 */
	public JmsOutboundGatewaySpec extractReplyPayload(boolean extractPayload) {
		this.target.setExtractReplyPayload(extractPayload);
		return _this();
	}

	/**
	 * @param headerMapper the headerMapper.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setHeaderMapper(JmsHeaderMapper)
	 */
	public JmsOutboundGatewaySpec headerMapper(JmsHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * @param destination the destination.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setRequestDestination(Destination)
	 */
	public JmsOutboundGatewaySpec requestDestination(Destination destination) {
		this.target.setRequestDestination(destination);
		return _this();
	}

	/**
	 * @param destination the destination name.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setRequestDestinationName(String)
	 */
	public JmsOutboundGatewaySpec requestDestination(String destination) {
		this.target.setRequestDestinationName(destination);
		return _this();
	}

	/**
	 * @param destination the destination expression.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setRequestDestinationExpression(org.springframework.expression.Expression)
	 */
	public JmsOutboundGatewaySpec requestDestinationExpression(String destination) {
		this.target.setRequestDestinationExpression(PARSER.parseExpression(destination));
		return _this();
	}

	/**
	 * Configure a {@link Function} that will be invoked at runtime to determine the destination to
	 * which a message will be sent. Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .<Foo>destination(m -> m.getPayload().getState())
	 * }
	 * </pre>
	 * @param destinationFunction the destination function.
	 * @param <P> the expected payload type.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setRequestDestinationExpression(org.springframework.expression.Expression)
	 * @see FunctionExpression
	 */
	public <P> JmsOutboundGatewaySpec requestDestination(Function<Message<P>, ?> destinationFunction) {
		this.target.setRequestDestinationExpression(new FunctionExpression<Message<P>>(destinationFunction));
		return _this();
	}

	/**
	 * @param destination the destination.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setReplyDestination(Destination)
	 */
	public JmsOutboundGatewaySpec replyDestination(Destination destination) {
		this.target.setReplyDestination(destination);
		return _this();
	}

	/**
	 * @param destination the destination name.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setReplyDestinationName(String)
	 */
	public JmsOutboundGatewaySpec replyDestination(String destination) {
		this.target.setReplyDestinationName(destination);
		return _this();
	}

	/**
	 * @param destination the destination expression.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setReplyDestinationExpression(org.springframework.expression.Expression)
	 */
	public JmsOutboundGatewaySpec replyDestinationExpression(String destination) {
		this.target.setReplyDestinationExpression(PARSER.parseExpression(destination));
		return _this();
	}

	/**
	 * Configure a {@link Function} that will be invoked at run time to determine the destination from
	 * which a reply will be received. Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .<Foo>replyDestination(m -> m.getPayload().getState())
	 * }
	 * </pre>
	 * @param destinationFunction the destination function.
	 * @param <P> the expected payload type.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setReplyDestinationExpression(org.springframework.expression.Expression)
	 * @see FunctionExpression
	 */
	public <P> JmsOutboundGatewaySpec replyDestination(Function<Message<P>, ?> destinationFunction) {
		this.target.setReplyDestinationExpression(new FunctionExpression<Message<P>>(destinationFunction));
		return _this();
	}

	/**
	 * @param destinationResolver the destinationResolver.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setDestinationResolver(DestinationResolver)
	 */
	public JmsOutboundGatewaySpec destinationResolver(DestinationResolver destinationResolver) {
		this.target.setDestinationResolver(destinationResolver);
		return _this();
	}

	/**
	 * @param messageConverter the messageConverter.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setMessageConverter(MessageConverter)
	 */
	public JmsOutboundGatewaySpec jmsMessageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * @param correlationKey the correlationKey
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setCorrelationKey(String)
	 */
	public JmsOutboundGatewaySpec correlationKey(String correlationKey) {
		this.target.setCorrelationKey(correlationKey);
		return _this();
	}

	/**
	 * @param pubSubDomain the pubSubDomain
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setReplyPubSubDomain(boolean)
	 */
	public JmsOutboundGatewaySpec requestPubSubDomain(boolean pubSubDomain) {
		this.target.setRequestPubSubDomain(pubSubDomain);
		return _this();
	}

	/**
	 * @param pubSubDomain the pubSubDomain
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setRequestPubSubDomain(boolean)
	 */
	public JmsOutboundGatewaySpec replyPubSubDomain(boolean pubSubDomain) {
		this.target.setReplyPubSubDomain(pubSubDomain);
		return _this();
	}

	/**
	 * @param deliveryPersistent the deliveryPersistent.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setDeliveryPersistent(boolean)
	 */
	public JmsOutboundGatewaySpec deliveryPersistent(boolean deliveryPersistent) {
		this.target.setDeliveryPersistent(deliveryPersistent);
		return _this();
	}

	/**
	 * Default priority. May be overridden at run time with a message
	 * priority header.
	 * @param priority the priority.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setDefaultPriority(int)
	 */
	public JmsOutboundGatewaySpec priority(int priority) {
		this.target.setDefaultPriority(priority);
		return _this();
	}

	/**
	 * @param timeToLive the timeToLive.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setTimeToLive(long)
	 */
	public JmsOutboundGatewaySpec timeToLive(long timeToLive) {
		this.target.setTimeToLive(timeToLive);
		return _this();
	}

	/**
	 * @param receiveTimeout the receiveTimeout.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setReceiveTimeout(long)
	 */
	public JmsOutboundGatewaySpec receiveTimeout(long receiveTimeout) {
		this.target.setReceiveTimeout(receiveTimeout);
		return _this();
	}

	/**
	 * @param explicitQosEnabled the explicitQosEnabled.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 * @see JmsOutboundGateway#setExplicitQosEnabled(boolean)
	 */
	public JmsOutboundGatewaySpec explicitQosEnabled(boolean explicitQosEnabled) {
		this.target.setExplicitQosEnabled(explicitQosEnabled);
		return _this();
	}

	/**
	 * Configure a reply container with default properties.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 */
	public JmsOutboundGatewaySpec replyContainer() {
		this.target.setReplyContainerProperties(new JmsOutboundGateway.ReplyContainerProperties());
		return _this();
	}

	/**
	 * Configure a reply container with a reply container specification determined by
	 * invoking the {@link Consumer} callback with a {@link ReplyContainerSpec}.
	 * @param configurer the configurer.
	 * @return the current {@link JmsOutboundGatewaySpec}.
	 */
	public JmsOutboundGatewaySpec replyContainer(Consumer<ReplyContainerSpec> configurer) {
		Assert.notNull(configurer, "'configurer' must not be null");
		ReplyContainerSpec spec = new ReplyContainerSpec();
		configurer.accept(spec);
		this.target.setReplyContainerProperties(spec.getObject());
		return _this();
	}

	/**
	 * An {@link IntegrationComponentSpec} for {@link JmsOutboundGateway.ReplyContainerProperties}.
	 *
	 */
	public class ReplyContainerSpec
			extends IntegrationComponentSpec<ReplyContainerSpec, JmsOutboundGateway.ReplyContainerProperties> {

		ReplyContainerSpec() {
			this.target = new JmsOutboundGateway.ReplyContainerProperties();
		}

		/**
		 * @param sessionTransacted the sessionTransacted.
		 * @return the current {@link ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setSessionTransacted(boolean)
		 */
		public ReplyContainerSpec sessionTransacted(Boolean sessionTransacted) {
			this.target.setSessionTransacted(sessionTransacted);
			return _this();
		}

		/**
		 * @param sessionAcknowledgeMode the acknowledgement mode constant
		 * @return the current {@link ReplyContainerSpec}.
		 * @see jakarta.jms.Session#AUTO_ACKNOWLEDGE etc.
		 */
		public ReplyContainerSpec sessionAcknowledgeMode(Integer sessionAcknowledgeMode) {
			this.target.setSessionAcknowledgeMode(sessionAcknowledgeMode);
			return _this();
		}

		/**
		 * @param receiveTimeout the receiveTimeout.
		 * @return the current {@link ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setReceiveTimeout(long)
		 */
		public ReplyContainerSpec receiveTimeout(Long receiveTimeout) {
			this.target.setReceiveTimeout(receiveTimeout);
			return _this();
		}

		/**
		 * @param recoveryInterval the recoveryInterval.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setRecoveryInterval(long)
		 */
		public ReplyContainerSpec recoveryInterval(Long recoveryInterval) {
			this.target.setRecoveryInterval(recoveryInterval);
			return _this();
		}

		/**
		 * @param cacheLevel the value for
		 * {@code org.springframework.jms.listener.DefaultMessageListenerContainer.cacheLevel}.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setCacheLevel(int)
		 */
		public ReplyContainerSpec cacheLevel(Integer cacheLevel) {
			this.target.setCacheLevel(cacheLevel);
			return _this();
		}

		/**
		 * @param concurrentConsumers the concurrentConsumers.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setConcurrentConsumers(int)
		 */
		public ReplyContainerSpec concurrentConsumers(Integer concurrentConsumers) {
			this.target.setConcurrentConsumers(concurrentConsumers);
			return _this();
		}

		/**
		 * @param maxConcurrentConsumers the maxConcurrentConsumers.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setMaxConcurrentConsumers(int)
		 */
		public ReplyContainerSpec maxConcurrentConsumers(Integer maxConcurrentConsumers) {
			this.target.setMaxConcurrentConsumers(maxConcurrentConsumers);
			return _this();
		}

		/**
		 * @param maxMessagesPerTask the maxMessagesPerTask.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setMaxMessagesPerTask(int)
		 */
		public ReplyContainerSpec maxMessagesPerTask(Integer maxMessagesPerTask) {
			this.target.setMaxMessagesPerTask(maxMessagesPerTask);
			return _this();
		}

		/**
		 * @param idleConsumerLimit the idleConsumerLimit.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setIdleConsumerLimit(int)
		 */
		public ReplyContainerSpec idleConsumerLimit(Integer idleConsumerLimit) {
			this.target.setIdleConsumerLimit(idleConsumerLimit);
			return _this();
		}

		/**
		 * @param idleTaskExecutionLimit the idleTaskExecutionLimit.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setIdleTaskExecutionLimit(int)
		 */
		public ReplyContainerSpec idleTaskExecutionLimit(Integer idleTaskExecutionLimit) {
			this.target.setIdleTaskExecutionLimit(idleTaskExecutionLimit);
			return _this();
		}

		/**
		 * @param taskExecutor the taskExecutor.
		 * @return the current {ReplyContainerSpec}.
		 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setTaskExecutor(Executor)
		 */
		public ReplyContainerSpec taskExecutor(Executor taskExecutor) {
			this.target.setTaskExecutor(taskExecutor);
			return _this();
		}

		/**
		 * @param idleReplyContainerTimeout the timeout in seconds.
		 * @return the current {ReplyContainerSpec}.
		 * @see JmsOutboundGateway#setIdleReplyContainerTimeout
		 */
		public ReplyContainerSpec idleReplyContainerTimeout(long idleReplyContainerTimeout) {
			JmsOutboundGatewaySpec.this.target.setIdleReplyContainerTimeout(idleReplyContainerTimeout);
			return this;
		}

	}

}
