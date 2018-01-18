/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.jms;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.jms.util.JmsAdapterUtils;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * An outbound Messaging Gateway for request/reply JMS.
 *
 * @author Mark Fisher
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JmsOutboundGateway extends AbstractReplyProducingMessageHandler implements Lifecycle, MessageListener {

	private final Object initializationMonitor = new Object();

	private final AtomicLong correlationId = new AtomicLong();

	private final String gatewayCorrelation = UUID.randomUUID().toString();

	private final Map<String, LinkedBlockingQueue<javax.jms.Message>> replies =
			new ConcurrentHashMap<String, LinkedBlockingQueue<javax.jms.Message>>();

	private final ConcurrentHashMap<String, TimedReply> earlyOrLateReplies =
			new ConcurrentHashMap<String, JmsOutboundGateway.TimedReply>();

	private final Map<String, SettableListenableFuture<AbstractIntegrationMessageBuilder<?>>> futures =
			new ConcurrentHashMap<String, SettableListenableFuture<AbstractIntegrationMessageBuilder<?>>>();

	private final Object lifeCycleMonitor = new Object();

	private volatile Destination requestDestination;

	private volatile String requestDestinationName;

	private volatile ExpressionEvaluatingMessageProcessor<?> requestDestinationExpressionProcessor;

	private volatile Destination replyDestination;

	private volatile String replyDestinationName;

	private volatile ExpressionEvaluatingMessageProcessor<?> replyDestinationExpressionProcessor;

	private volatile DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private volatile boolean requestPubSubDomain;

	private volatile boolean replyPubSubDomain;

	private volatile long receiveTimeout = 5000;

	private volatile int deliveryMode = javax.jms.Message.DEFAULT_DELIVERY_MODE;

	private volatile long timeToLive = javax.jms.Message.DEFAULT_TIME_TO_LIVE;

	private volatile int priority = javax.jms.Message.DEFAULT_PRIORITY;

	private volatile boolean explicitQosEnabled;

	private ConnectionFactory connectionFactory;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();

	private volatile String correlationKey;

	private volatile boolean extractRequestPayload = true;

	private volatile boolean extractReplyPayload = true;

	private volatile boolean initialized;

	private volatile GatewayReplyListenerContainer replyContainer;

	private volatile ReplyContainerProperties replyContainerProperties;

	private volatile boolean useReplyContainer;

	private volatile boolean active;

	private volatile ScheduledFuture<?> reaper;

	private volatile boolean requiresReply;

	private long lastSend;

	private volatile long idleReplyContainerTimeout;

	private volatile boolean wasStopped;

	private ScheduledFuture<?> idleTask;

	/**
	 * Set whether message delivery should be persistent or non-persistent,
	 * specified as a boolean value ("true" or "false"). This will set the delivery
	 * mode accordingly to either "PERSISTENT" (1) or "NON_PERSISTENT" (2).
	 * <p>The default is "true", i.e. delivery mode "PERSISTENT".
	 *
	 * @param deliveryPersistent true for a persistent delivery.
	 *
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 */
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.deliveryMode = (deliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
	}

	/**
	 * Set the JMS ConnectionFactory that this gateway should use.
	 * This is a <em>required</em> property.
	 *
	 * @param connectionFactory The connection factory.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Set the JMS Destination to which request Messages should be sent.
	 * Either this or one of 'requestDestinationName' or 'requestDestinationExpression' is required.
	 *
	 * @param requestDestination The request destination.
	 */
	public void setRequestDestination(Destination requestDestination) {
		if (requestDestination instanceof Topic) {
			this.requestPubSubDomain = true;
		}
		this.requestDestination = requestDestination;
	}

	/**
	 * Set the name of the JMS Destination to which request Messages should be sent.
	 * Either this or one of 'requestDestination' or 'requestDestinationExpression' is required.
	 *
	 * @param requestDestinationName The request destination name.
	 */
	public void setRequestDestinationName(String requestDestinationName) {
		this.requestDestinationName = requestDestinationName;
	}

	/**
	 * Set the SpEL Expression to be used for determining the request Destination instance
	 * or request destination name. Either this or one of 'requestDestination' or
	 * 'requestDestinationName' is required.
	 *
	 * @param requestDestinationExpression The request destination expression.
	 */
	public void setRequestDestinationExpression(Expression requestDestinationExpression) {
		Assert.notNull(requestDestinationExpression, "'requestDestinationExpression' must not be null");
		this.requestDestinationExpressionProcessor =
				new ExpressionEvaluatingMessageProcessor<Object>(requestDestinationExpression);
		setPrimaryExpression(requestDestinationExpression);
	}

	/**
	 * Set the JMS Destination from which reply Messages should be received.
	 * If none is provided, this gateway will create a {@link TemporaryQueue} per invocation.
	 *
	 * @param replyDestination The reply destination.
	 */
	public void setReplyDestination(Destination replyDestination) {
		if (replyDestination instanceof Topic) {
			this.replyPubSubDomain = true;
		}
		this.replyDestination = replyDestination;
	}

	/**
	 * Set the name of the JMS Destination from which reply Messages should be received.
	 * If none is provided, this gateway will create a {@link TemporaryQueue} per invocation.
	 *
	 * @param replyDestinationName The reply destination name.
	 */
	public void setReplyDestinationName(String replyDestinationName) {
		this.replyDestinationName = replyDestinationName;
	}

	/**
	 * Set the SpEL Expression to be used for determining the reply Destination instance
	 * or reply destination name. Either this or one of 'replyDestination' or
	 * 'replyDestinationName' is required.
	 *
	 * @param replyDestinationExpression The reply destination expression.
	 */
	public void setReplyDestinationExpression(Expression replyDestinationExpression) {
		Assert.notNull(replyDestinationExpression, "'replyDestinationExpression' must not be null");
		this.replyDestinationExpressionProcessor = new ExpressionEvaluatingMessageProcessor<Object>(replyDestinationExpression);
	}

	/**
	 * Provide the {@link DestinationResolver} to use when resolving either a
	 * 'requestDestinationName' or 'replyDestinationName' value. The default
	 * is an instance of {@link DynamicDestinationResolver}.
	 *
	 * @param destinationResolver The destination resolver.
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Specify whether the request destination is a Topic. This value is
	 * necessary when providing a destination name for a Topic rather than
	 * a destination reference.
	 *
	 * @param requestPubSubDomain true if the request destination is a Topic.
	 */
	public void setRequestPubSubDomain(boolean requestPubSubDomain) {
		this.requestPubSubDomain = requestPubSubDomain;
	}

	/**
	 * Specify whether the reply destination is a Topic. This value is
	 * necessary when providing a destination name for a Topic rather than
	 * a destination reference.
	 *
	 * @param replyPubSubDomain true if the reply destination is a Topic.
	 */
	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	/**
	 * Set the max timeout value for the MessageConsumer's receive call when
	 * waiting for a reply. The default value is 5 seconds.
	 *
	 * @param receiveTimeout The receive timeout.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Specify the JMS priority to use when sending request Messages.
	 * The value should be within the range of 0-9.
	 *
	 * @param priority The priority.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Specify the timeToLive for each sent Message.
	 * The default value indicates no expiration.
	 *
	 * @param timeToLive The time to live.
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * Specify whether explicit QoS settings are enabled
	 * (deliveryMode, priority, and timeToLive).
	 *
	 * @param explicitQosEnabled true to enable explicit QoS.
	 */
	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.explicitQosEnabled = explicitQosEnabled;
	}

	/**
	 * Provide the name of a JMS property that should hold a generated UUID that
	 * the receiver of the JMS Message would expect to represent the CorrelationID.
	 * When waiting for the reply Message, a MessageSelector will be configured
	 * to match this property name and the UUID value that was sent in the request.
	 * <p>If this value is NULL (the default) then the reply consumer's MessageSelector
	 * will be expecting the JMSCorrelationID to equal the Message ID of the request.
	 * <p>If you want to store the outbound correlation UUID value in the actual
	 * JMSCorrelationID property, then set this value to "JMSCorrelationID".
	 * <p>If you want to use and existing "JMSCorrelationID" from the inbound message
	 * (mapped from 'jms_correlationId'),
	 * you can set this property to "JMSCorrelationID*" with the trailing asterisk.
	 * If the message has a correlation id, it will be used, otherwise a new one will
	 * be set in the 'JMSCorrelationID' header. However, understand that the
	 * gateway has no means to ensure uniqueness and unexpected side effects can
	 * occur if the correlation id is not unique.
	 * <p>This setting is not allowed if a reply listener is used.
	 * @param correlationKey The correlation key.
	 */
	public void setCorrelationKey(String correlationKey) {
		this.correlationKey = correlationKey;
	}

	/**
	 * Provide a {@link MessageConverter} strategy to use for converting the
	 * Spring Integration request Message into a JMS Message and for converting
	 * the JMS reply Messages back into Spring Integration Messages.
	 * <p>
	 * The default is {@link SimpleMessageConverter}.
	 *
	 * @param messageConverter The message converter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * Provide a {@link JmsHeaderMapper} implementation for mapping the
	 * Spring Integration Message Headers to/from JMS Message properties.
	 *
	 * @param headerMapper The header mapper.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * This property describes how a JMS Message should be generated from the
	 * Spring Integration Message. If set to 'true', the body of the JMS Message will be
	 * created from the Spring Integration Message's payload (via the MessageConverter).
	 * If set to 'false', then the entire Spring Integration Message will serve as
	 * the base for JMS Message creation. Since the JMS Message is created by the
	 * MessageConverter, this really manages what is sent to the {@link MessageConverter}:
	 * the entire Spring Integration Message or only its payload.
	 * <br>
	 * Default is 'true'
	 *
	 * @param extractRequestPayload true to extract the request payload.
	 */
	public void setExtractRequestPayload(boolean extractRequestPayload) {
		this.extractRequestPayload = extractRequestPayload;
	}

	/**
	 * This property describes what to do with a JMS reply Message.
	 * If set to 'true', the payload of the Spring Integration Message will be
	 * created from the JMS Reply Message's body (via MessageConverter).
	 * Otherwise, the entire JMS Message will become the payload of the
	 * Spring Integration Message.
	 *
	 * @param extractReplyPayload true to extract the reply payload.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	/**
	 * Specify the Spring Integration reply channel. If this property is not
	 * set the gateway will check for a 'replyChannel' header on the request.
	 *
	 * @param replyChannel The reply channel.
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	/**
	 * @param replyContainerProperties the replyContainerProperties to set
	 */
	public void setReplyContainerProperties(ReplyContainerProperties replyContainerProperties) {
		this.replyContainerProperties = replyContainerProperties;
		this.useReplyContainer = true;
	}

	@Override
	public String getComponentType() {
		return "jms:outbound-gateway";
	}

	/**
	 * @param useReplyContainer the useReplyContainer to set
	 */
	public void setUseReplyContainer(boolean useReplyContainer) {
		this.useReplyContainer = useReplyContainer;
	}

	@Override
	public void setRequiresReply(boolean requiresReply) {
		super.setRequiresReply(requiresReply);
		this.requiresReply = requiresReply;
	}

	/**
	 * Set the target timeout for idle containers, in seconds. Setting this greater than zero enables lazy
	 * starting of the reply listener container. The container will be started when a message is sent. It will be
	 * stopped when idle for at least this time. The actual stop time may be up to 1.5x this time.
	 * @param idleReplyContainerTimeout the timeout in seconds.
	 * @since 4.2
	 */
	public void setIdleReplyContainerTimeout(long idleReplyContainerTimeout) {
		setIdleReplyContainerTimeout(idleReplyContainerTimeout, TimeUnit.SECONDS);
	}

	/**
	 * Set the target timeout for idle containers. Setting this greater than zero enables lazy
	 * starting of the reply listener container. The container will be started when a message is sent. It will be
	 * stopped when idle for at least this time. The actual stop time may be up to 1.5x this time.
	 * @param idleReplyContainerTimeout the timeout in seconds.
	 * @param unit the time unit.
	 * @since 4.2
	 */
	public void setIdleReplyContainerTimeout(long idleReplyContainerTimeout, TimeUnit unit) {
		this.idleReplyContainerTimeout = unit.toMillis(idleReplyContainerTimeout);
	}

	private Destination determineRequestDestination(Message<?> message, Session session) throws JMSException {
		if (this.requestDestination != null) {
			return this.requestDestination;
		}
		if (this.requestDestinationName != null) {
			return this.resolveRequestDestination(this.requestDestinationName, session);
		}
		if (this.requestDestinationExpressionProcessor != null) {
			Object result = this.requestDestinationExpressionProcessor.processMessage(message);
			if (result instanceof Destination) {
				return (Destination) result;
			}
			if (result instanceof String) {
				return this.resolveRequestDestination((String) result, session);
			}
			throw new MessageDeliveryException(message,
					"Evaluation of requestDestinationExpression failed " +
							"to produce a Destination or destination name. Result was: " + result);
		}
		throw new MessageDeliveryException(message,
				"No requestDestination, requestDestinationName, or requestDestinationExpression has been configured.");
	}

	private Destination resolveRequestDestination(String requestDestinationName, Session session) throws JMSException {
		Assert.notNull(this.destinationResolver,
				"DestinationResolver is required when relying upon the 'requestDestinationName' property.");
		return this.destinationResolver.resolveDestinationName(
				session, requestDestinationName, this.requestPubSubDomain);
	}

	private Destination determineReplyDestination(Message<?> message, Session session) throws JMSException {
		if (this.replyDestination != null) {
			return this.replyDestination;
		}
		if (this.replyDestinationName != null) {
			return this.resolveReplyDestination(this.replyDestinationName, session);
		}
		if (this.replyDestinationExpressionProcessor != null) {
			Object result = this.replyDestinationExpressionProcessor.processMessage(message);
			if (result instanceof Destination) {
				return (Destination) result;
			}
			if (result instanceof String) {
				return this.resolveReplyDestination((String) result, session);
			}
			throw new MessageDeliveryException(message,
					"Evaluation of replyDestinationExpression failed to produce a Destination or destination name. " +
							"Result was: " + result);
		}
		return session.createTemporaryQueue();
	}

	private Destination resolveReplyDestination(String replyDestinationName, Session session) throws JMSException {
		Assert.notNull(this.destinationResolver,
				"DestinationResolver is required when relying upon the 'replyDestinationName' property.");
		return this.destinationResolver.resolveDestinationName(
				session, replyDestinationName, this.replyPubSubDomain);
	}

	@Override
	protected void doInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.connectionFactory, "connectionFactory must not be null");
			Assert.isTrue(this.requestDestination != null
							^ this.requestDestinationName != null
							^ this.requestDestinationExpressionProcessor != null,
					"Exactly one of 'requestDestination', 'requestDestinationName', " +
							"or 'requestDestinationExpression' is required.");
			ConversionService conversionService = getConversionService();
			if (this.requestDestinationExpressionProcessor != null) {
				this.requestDestinationExpressionProcessor.setBeanFactory(getBeanFactory());
				if (conversionService != null) {
					this.requestDestinationExpressionProcessor.setConversionService(conversionService);
				}
			}
			if (this.replyDestinationExpressionProcessor != null) {
				this.replyDestinationExpressionProcessor.setBeanFactory(getBeanFactory());
				if (conversionService != null) {
					this.replyDestinationExpressionProcessor.setConversionService(conversionService);
				}
			}
			/*
			 *  This is needed because there is no way to detect 2 or more gateways using the same reply queue
			 *  with no correlation key.
			 */
			if (this.useReplyContainer && (this.correlationKey == null &&
					(this.replyDestination != null || this.replyDestinationName != null) ||
					this.replyDestinationExpressionProcessor != null)) {
				if (logger.isWarnEnabled()) {
					logger.warn("The gateway cannot use a reply listener container with a specified " +
							"destination(Name/Expression) " +
							"without a 'correlation-key'; " +
							"a container will NOT be used; " +
							"to avoid this problem, set the 'correlation-key' attribute; " +
							"some consumers, including the Spring Integration <jms:inbound-gateway/>, " +
							"support the use of the value 'JMSCorrelationID' " +
							"for this purpose. Alternatively, do not specify a reply destination " +
							"and a temporary queue will be used for replies.");
				}
				this.useReplyContainer = false;
			}
			if (this.useReplyContainer) {
				Assert.state(!"JMSCorrelationID*".equals(this.correlationKey),
						"Using an existing 'JMSCorrelationID' mapped from the 'requestMessage' ('JMSCorrelationID*') " +
								"can't be used when using a 'reply-container'");
				GatewayReplyListenerContainer container = new GatewayReplyListenerContainer();
				setContainerProperties(container);
				container.afterPropertiesSet();
				this.replyContainer = container;
				if (isAsync() && this.correlationKey == null) {
					logger.warn("'async=true' requires a correlationKey; ignored");
					setAsync(false);
				}
			}
			else {
				if (isAsync()) {
					logger.warn("'async=true' is ignored when a reply container is not being used");
					setAsync(false);
				}
			}
			this.initialized = true;
		}
	}

	private void setContainerProperties(GatewayReplyListenerContainer container) {
		container.setConnectionFactory(this.connectionFactory);
		if (this.replyDestination != null) {
			container.setDestination(this.replyDestination);
		}
		else if (StringUtils.hasText(this.replyDestinationName)) {
			container.setDestinationName(this.replyDestinationName);
		}
		else {
			// to be resolved to the TemporaryQueue
			container.setDestinationName("");
		}
		if (this.destinationResolver != null) {
			container.setDestinationResolver(this.destinationResolver);
		}
		container.setPubSubDomain(this.replyPubSubDomain);
		if (this.correlationKey != null) {
			String messageSelector = this.correlationKey + " LIKE '" + this.gatewayCorrelation + "%'";
			container.setMessageSelector(messageSelector);
		}
		container.setMessageListener(this);
		if (this.replyContainerProperties != null) {
			if (this.replyContainerProperties.isSessionTransacted() != null) {
				container.setSessionTransacted(this.replyContainerProperties.isSessionTransacted());
			}
			if (this.replyContainerProperties.getCacheLevel() != null) {
				container.setCacheLevel(this.replyContainerProperties.getCacheLevel());
			}
			if (this.replyContainerProperties.getConcurrentConsumers() != null) {
				container.setConcurrentConsumers(this.replyContainerProperties.getConcurrentConsumers());
			}
			if (this.replyContainerProperties.getIdleConsumerLimit() != null) {
				container.setIdleConsumerLimit(this.replyContainerProperties.getIdleConsumerLimit());
			}
			if (this.replyContainerProperties.getIdleTaskExecutionLimit() != null) {
				container.setIdleTaskExecutionLimit(this.replyContainerProperties.getIdleTaskExecutionLimit());
			}
			if (this.replyContainerProperties.getMaxConcurrentConsumers() != null) {
				container.setMaxConcurrentConsumers(this.replyContainerProperties.getMaxConcurrentConsumers());
			}
			if (this.replyContainerProperties.getMaxMessagesPerTask() != null) {
				container.setMaxMessagesPerTask(this.replyContainerProperties.getMaxMessagesPerTask());
			}
			if (this.replyContainerProperties.getReceiveTimeout() != null) {
				container.setReceiveTimeout(this.replyContainerProperties.getReceiveTimeout());
			}
			if (this.replyContainerProperties.getRecoveryInterval() != null) {
				container.setRecoveryInterval(this.replyContainerProperties.getRecoveryInterval());
			}
			if (StringUtils.hasText(this.replyContainerProperties.getSessionAcknowledgeModeName())) {
				Integer acknowledgeMode = JmsAdapterUtils.parseAcknowledgeMode(
						this.replyContainerProperties.getSessionAcknowledgeModeName());
				if (acknowledgeMode != null) {
					if (JmsAdapterUtils.SESSION_TRANSACTED == acknowledgeMode) {
						container.setSessionTransacted(true);
					}
					else {
						container.setSessionAcknowledgeMode(acknowledgeMode);
					}
				}
			}
			else if (this.replyContainerProperties.getSessionAcknowledgeMode() != null) {
				Integer sessionAcknowledgeMode = this.replyContainerProperties.getSessionAcknowledgeMode();
				if (Session.SESSION_TRANSACTED == sessionAcknowledgeMode) {
					container.setSessionTransacted(true);
				}
				else {
					container.setSessionAcknowledgeMode(sessionAcknowledgeMode);
				}

			}

			if (this.replyContainerProperties.getTaskExecutor() != null) {
				container.setTaskExecutor(this.replyContainerProperties.getTaskExecutor());
			}
			else {
				// set the beanName so the default TE threads get a meaningful name
				String containerBeanName = this.getComponentName();
				containerBeanName = ((!StringUtils.hasText(containerBeanName)
						? "JMS_OutboundGateway@" + ObjectUtils.getIdentityHexString(this)
						: containerBeanName) + ".replyListener");
				container.setBeanName(containerBeanName);
			}
		}
	}


	@Override
	public void start() {
		synchronized (this.lifeCycleMonitor) {
			if (!this.active) {
				if (this.replyContainer != null) {
					TaskScheduler taskScheduler = getTaskScheduler();
					if (this.idleReplyContainerTimeout <= 0) {
						if (this.wasStopped) {
							this.replyContainer.initialize();
							this.wasStopped = false;
						}
						this.replyContainer.start();
					}
					else {
						Assert.state(taskScheduler != null, "'taskScheduler' is required.");
					}
					if (!isAsync() && this.receiveTimeout >= 0) {
						Assert.state(taskScheduler != null, "'taskScheduler' is required.");
						this.reaper = taskScheduler.schedule(new LateReplyReaper(), new Date());
					}
				}
				this.active = true;
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this.lifeCycleMonitor) {
			if (this.replyContainer != null) {
				this.replyContainer.shutdown();
				this.wasStopped = true;
				this.deleteDestinationIfTemporary(this.replyContainer.getDestination());
				if (this.reaper != null) {
					this.reaper.cancel(false);
				}
			}
			if (this.idleTask != null) {
				this.idleTask.cancel(true);
				this.idleTask = null;
			}
			this.active = false;
		}
	}

	@Override
	public boolean isRunning() {
		return this.active;
	}

	@Override
	protected Object handleRequestMessage(final Message<?> requestMessage) {
		if (!this.initialized) {
			afterPropertiesSet();
		}
		try {
			Object reply;
			if (this.replyContainer == null) {
				reply = sendAndReceiveWithoutContainer(requestMessage);
			}
			else {
				if (this.idleReplyContainerTimeout > 0) {
					synchronized (this.lifeCycleMonitor) {
						this.lastSend = System.currentTimeMillis();
						if (!this.replyContainer.isRunning()) {
							if (logger.isDebugEnabled()) {
								logger.debug(this.getComponentName() + ": Starting reply container.");
							}
							this.replyContainer.start();
							this.idleTask = getTaskScheduler().scheduleAtFixedRate(new IdleContainerStopper(),
									this.idleReplyContainerTimeout / 2);
						}
					}
				}
				reply = this.sendAndReceiveWithContainer(requestMessage);
			}
			if (reply == null) {
				if (this.requiresReply) {
					throw new MessageTimeoutException(requestMessage,
							"failed to receive JMS response within timeout of: " + this.receiveTimeout + "ms");
				}
				else {
					return null;
				}
			}

			if (reply instanceof javax.jms.Message) {
				return buildReply((javax.jms.Message) reply);
			}
			else {
				return reply;
			}
		}
		catch (JMSException e) {
			throw new MessageHandlingException(requestMessage, e);
		}
	}

	private AbstractIntegrationMessageBuilder<?> buildReply(javax.jms.Message jmsReply) throws JMSException {
		Object result = jmsReply;
		if (this.extractReplyPayload) {
			result = this.messageConverter.fromMessage(jmsReply);
			if (logger.isDebugEnabled()) {
				logger.debug("converted JMS Message [" + jmsReply + "] to integration Message payload [" + result + "]");
			}
		}
		Map<String, Object> jmsReplyHeaders = this.headerMapper.toHeaders(jmsReply);

		if (this.replyContainer != null && this.correlationKey != null) {
			// do not propagate back the gateway's internal correlation id
			jmsReplyHeaders.remove(this.correlationKey);
		}
		if (result instanceof Message) {
			return getMessageBuilderFactory().fromMessage((Message<?>) result).copyHeaders(jmsReplyHeaders);
		}
		else {
			return getMessageBuilderFactory().withPayload(result).copyHeaders(jmsReplyHeaders);
		}
	}

	private Object sendAndReceiveWithContainer(Message<?> requestMessage) throws JMSException {
		Connection connection = this.createConnection(); // NOSONAR - closed in ConnectionFactoryUtils.
		Session session = null;
		Destination replyTo = this.replyContainer.getReplyDestination();
		try {
			session = this.createSession(connection);

			// convert to JMS Message
			Object objectToSend = requestMessage;
			if (this.extractRequestPayload) {
				objectToSend = requestMessage.getPayload();
			}
			javax.jms.Message jmsRequest = this.messageConverter.toMessage(objectToSend, session);

			// map headers
			this.headerMapper.fromHeaders(requestMessage.getHeaders(), jmsRequest);

			jmsRequest.setJMSReplyTo(replyTo);
			connection.start();
			if (logger.isDebugEnabled()) {
				logger.debug("ReplyTo: " + replyTo);
			}

			Integer priority = new IntegrationMessageHeaderAccessor(requestMessage).getPriority();
			if (priority == null) {
				priority = this.priority;
			}
			Destination requestDestination = this.determineRequestDestination(requestMessage, session);

			Object reply = null;
			if (this.correlationKey == null) {
				/*
				 * Remove any existing correlation id that was mapped from the inbound message
				 * (it will be restored in the reply by normal ARPMH header processing).
				 */
				jmsRequest.setJMSCorrelationID(null);
				reply = doSendAndReceiveAsyncDefaultCorrelation(requestDestination, jmsRequest, session, priority);
			}
			else {
				reply = doSendAndReceiveAsync(requestDestination, jmsRequest, session, priority);
			}
			/*
			 * Remove the gateway's internal correlation Id to avoid conflicts with an upstream
			 * gateway.
			 */
			if (reply instanceof javax.jms.Message) {
				((javax.jms.Message) reply).setJMSCorrelationID(null);
			}
			return reply;
		}
		finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(connection, this.connectionFactory, true);
		}
	}

	private javax.jms.Message sendAndReceiveWithoutContainer(Message<?> requestMessage) throws JMSException {
		Connection connection = this.createConnection(); // NOSONAR - closed in ConnectionFactoryUtils.
		Session session = null;
		Destination replyTo = null;
		try {
			session = this.createSession(connection);

			// convert to JMS Message
			Object objectToSend = requestMessage;
			if (this.extractRequestPayload) {
				objectToSend = requestMessage.getPayload();
			}
			javax.jms.Message jmsRequest = this.messageConverter.toMessage(objectToSend, session);

			// map headers
			this.headerMapper.fromHeaders(requestMessage.getHeaders(), jmsRequest);

			replyTo = this.determineReplyDestination(requestMessage, session);
			jmsRequest.setJMSReplyTo(replyTo);
			connection.start();
			if (logger.isDebugEnabled()) {
				logger.debug("ReplyTo: " + replyTo);
			}

			Integer priority = new IntegrationMessageHeaderAccessor(requestMessage).getPriority();
			if (priority == null) {
				priority = this.priority;
			}
			javax.jms.Message replyMessage = null;
			Destination requestDestination = this.determineRequestDestination(requestMessage, session);
			if (this.correlationKey != null) {
				replyMessage = doSendAndReceiveWithGeneratedCorrelationId(requestDestination, jmsRequest, replyTo,
						session, priority);
			}
			else if (replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic) {
				replyMessage = doSendAndReceiveWithTemporaryReplyToDestination(requestDestination, jmsRequest, replyTo,
						session, priority);
			}
			else {
				replyMessage = doSendAndReceiveWithMessageIdCorrelation(requestDestination, jmsRequest, replyTo,
						session, priority);
			}
			return replyMessage;
		}
		finally {
			JmsUtils.closeSession(session);
			this.deleteDestinationIfTemporary(replyTo);
			ConnectionFactoryUtils.releaseConnection(connection, this.connectionFactory, true);
		}
	}

	/**
	 * Creates the MessageConsumer before sending the request Message since we are generating
	 * our own correlationId value for the MessageSelector.
	 */
	private javax.jms.Message doSendAndReceiveWithGeneratedCorrelationId(Destination requestDestination,
			javax.jms.Message jmsRequest, Destination replyTo, Session session, int priority) throws JMSException {
		MessageProducer messageProducer = null;
		try {
			messageProducer = session.createProducer(requestDestination);
			Assert.state(this.correlationKey != null, "correlationKey must not be null");
			String messageSelector = null;
			if (!this.correlationKey.equals("JMSCorrelationID*") || jmsRequest.getJMSCorrelationID() == null) {
				String correlationId = UUID.randomUUID().toString().replaceAll("'", "''");
				if (this.correlationKey.equals("JMSCorrelationID")) {
					jmsRequest.setJMSCorrelationID(correlationId);
					messageSelector = "JMSCorrelationID = '" + correlationId + "'";
				}
				else {
					jmsRequest.setStringProperty(this.correlationKey, correlationId);
					jmsRequest.setJMSCorrelationID(null);
					messageSelector = this.correlationKey + " = '" + correlationId + "'";
				}
			}
			else {
				messageSelector = "JMSCorrelationID = '" + jmsRequest.getJMSCorrelationID() + "'";
			}

			this.sendRequestMessage(jmsRequest, messageProducer, priority);
			return retryableReceiveReply(session, replyTo, messageSelector);
		}
		finally {
			JmsUtils.closeMessageProducer(messageProducer);
		}
	}

	/**
	 * Creates the MessageConsumer before sending the request Message since we do not need any correlation.
	 */
	private javax.jms.Message doSendAndReceiveWithTemporaryReplyToDestination(Destination requestDestination,
			javax.jms.Message jmsRequest, Destination replyTo, Session session, int priority) throws JMSException {
		MessageProducer messageProducer = null;
		MessageConsumer messageConsumer = null;
		try {
			messageProducer = session.createProducer(requestDestination);
			messageConsumer = session.createConsumer(replyTo);
			this.sendRequestMessage(jmsRequest, messageProducer, priority);
			return this.receiveReplyMessage(messageConsumer);
		}
		finally {
			JmsUtils.closeMessageProducer(messageProducer);
			JmsUtils.closeMessageConsumer(messageConsumer);
		}
	}

	/**
	 * Creates the MessageConsumer after sending the request Message since we need
	 * the MessageID for correlation with a MessageSelector.
	 */
	private javax.jms.Message doSendAndReceiveWithMessageIdCorrelation(Destination requestDestination,
			javax.jms.Message jmsRequest, Destination replyTo, Session session, int priority) throws JMSException {
		if (replyTo instanceof Topic && logger.isWarnEnabled()) {
			logger.warn("Relying on the MessageID for correlation is not recommended when using a Topic as the replyTo Destination " +
					"because that ID can only be provided to a MessageSelector after the request Message has been sent thereby " +
					"creating a race condition where a fast response might be sent before the MessageConsumer has been created. " +
					"Consider providing a value to the 'correlationKey' property of this gateway instead. Then the MessageConsumer " +
					"will be created before the request Message is sent.");
		}
		MessageProducer messageProducer = null;
		try {
			messageProducer = session.createProducer(requestDestination);
			this.sendRequestMessage(jmsRequest, messageProducer, priority);
			String messageId = jmsRequest.getJMSMessageID().replaceAll("'", "''");
			String messageSelector = "JMSCorrelationID = '" + messageId + "'";
			return retryableReceiveReply(session, replyTo, messageSelector);
		}
		finally {
			JmsUtils.closeMessageProducer(messageProducer);
		}
	}


	/*
	 * If the replyTo is not temporary, and the connection is lost while waiting for a reply, reconnect for
	 * up to receiveTimeout.
	 */
	private javax.jms.Message retryableReceiveReply(Session session, Destination replyTo, String messageSelector)
			throws JMSException {
		Connection consumerConnection = null; //NOSONAR
		Session consumerSession = session;
		MessageConsumer messageConsumer = null;
		JMSException exception = null;
		boolean isTemporaryReplyTo = replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic;
		long replyTimeout = isTemporaryReplyTo
				? Long.MIN_VALUE
				: this.receiveTimeout < 0
				? Long.MAX_VALUE
				: System.currentTimeMillis() + this.receiveTimeout;
		try {
			do {
				try {
					messageConsumer = consumerSession.createConsumer(replyTo, messageSelector);
					javax.jms.Message reply = receiveReplyMessage(messageConsumer);
					if (reply == null) {
						if (replyTimeout > System.currentTimeMillis()) {
							throw new JMSException("Consumer closed before timeout");
						}
					}
					return reply;
				}
				catch (JMSException e) {
					exception = e;
					if (logger.isDebugEnabled()) {
						logger.debug("Connection lost waiting for reply, retrying: " + e.getMessage());
					}
					do {
						try {
							consumerConnection = createConnection();
							consumerSession = createSession(consumerConnection);
							break;
						}
						catch (JMSException ee) {
							exception = ee;
							if (logger.isDebugEnabled()) {
								logger.debug("Could not reconnect, retrying: " + ee.getMessage());
							}
							try {
								Thread.sleep(1000);
							}
							catch (InterruptedException e1) {
								Thread.currentThread().interrupt();
								return null;
							}
						}
					}
					while (replyTimeout > System.currentTimeMillis());
				}
			}
			while (replyTimeout > System.currentTimeMillis());
			if (isTemporaryReplyTo) {
				return null;
			}
			else {
				throw exception;
			}
		}
		finally {
			if (consumerSession != session) {
				JmsUtils.closeSession(consumerSession);
				JmsUtils.closeConnection(consumerConnection);
			}
			JmsUtils.closeMessageConsumer(messageConsumer);
		}
	}

	private Object doSendAndReceiveAsync(Destination requestDestination, javax.jms.Message jmsRequest, Session session,
			int priority) throws JMSException {
		String correlationId = null;
		MessageProducer messageProducer = null;
		try {
			messageProducer = session.createProducer(requestDestination);
			correlationId = this.gatewayCorrelation + "_" + Long.toString(this.correlationId.incrementAndGet());
			if (this.correlationKey.equals("JMSCorrelationID")) {
				jmsRequest.setJMSCorrelationID(correlationId);
			}
			else {
				jmsRequest.setStringProperty(this.correlationKey, correlationId);
				/*
				 * Remove any existing correlation id that was mapped from the inbound message
				 * (it will be restored in the reply by normal ARPMH header processing).
				 */
				jmsRequest.setJMSCorrelationID(null);
			}
			LinkedBlockingQueue<javax.jms.Message> replyQueue = null;
			if (logger.isDebugEnabled()) {
				logger.debug(this.getComponentName() + " Sending message with correlationId " + correlationId);
			}
			SettableListenableFuture<AbstractIntegrationMessageBuilder<?>> future = null;
			boolean async = isAsync();
			if (!async) {
				replyQueue = new LinkedBlockingQueue<javax.jms.Message>(1);
				this.replies.put(correlationId, replyQueue);
			}
			else {
				future = createFuture(correlationId);
			}

			this.sendRequestMessage(jmsRequest, messageProducer, priority);

			if (async) {
				return future;
			}
			else {
				return obtainReplyFromContainer(correlationId, replyQueue);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(messageProducer);
			if (correlationId != null && !isAsync()) {
				this.replies.remove(correlationId);
			}
		}
	}

	private javax.jms.Message doSendAndReceiveAsyncDefaultCorrelation(Destination requestDestination,
			javax.jms.Message jmsRequest, Session session, int priority) throws JMSException {
		String correlationId = null;
		MessageProducer messageProducer = null;

		try {
			messageProducer = session.createProducer(requestDestination);
			LinkedBlockingQueue<javax.jms.Message> replyQueue = new LinkedBlockingQueue<javax.jms.Message>(1);

			this.sendRequestMessage(jmsRequest, messageProducer, priority);

			correlationId = jmsRequest.getJMSMessageID();

			if (logger.isDebugEnabled()) {
				logger.debug(this.getComponentName() + " Sent message with correlationId " + correlationId);
			}
			this.replies.put(correlationId, replyQueue);

			/*
			 * Check to see if the reply arrived before we obtained the correlationId
			 */
			synchronized (this.earlyOrLateReplies) {
				TimedReply timedReply = this.earlyOrLateReplies.remove(correlationId);
				if (timedReply != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Found early reply with correlationId " + correlationId);
					}
					replyQueue.add(timedReply.getReply());
				}
			}

			return obtainReplyFromContainer(correlationId, replyQueue);
		}
		finally {
			JmsUtils.closeMessageProducer(messageProducer);
			if (correlationId != null) {
				this.replies.remove(correlationId);
			}
		}
	}

	private javax.jms.Message obtainReplyFromContainer(String correlationId,
			LinkedBlockingQueue<javax.jms.Message> replyQueue) {
		javax.jms.Message reply = null;

		if (this.receiveTimeout < 0) {
			reply = replyQueue.poll();
		}
		else {
			try {
				reply = replyQueue.poll(this.receiveTimeout, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				logger.error("Interrupted while awaiting reply; treated as a timeout", e);
				Thread.currentThread().interrupt();
			}
		}
		if (logger.isDebugEnabled()) {
			if (reply == null) {
				logger.debug(this.getComponentName() + " Timed out waiting for reply with CorrelationId "
						+ correlationId);
			}
			else {
				logger.debug(this.getComponentName() + " Obtained reply with CorrelationId " + correlationId);
			}
		}
		return reply;
	}

	private SettableListenableFuture<AbstractIntegrationMessageBuilder<?>> createFuture(final String correlationId) {
		SettableListenableFuture<AbstractIntegrationMessageBuilder<?>> future =
				new SettableListenableFuture<AbstractIntegrationMessageBuilder<?>>();
		this.futures.put(correlationId, future);
		if (this.receiveTimeout > 0) {
			getTaskScheduler().schedule((Runnable) () -> expire(correlationId),
					new Date(System.currentTimeMillis() + this.receiveTimeout));
		}
		return future;
	}

	private void expire(String correlationId) {
		final SettableListenableFuture<AbstractIntegrationMessageBuilder<?>> future = this.futures.remove(correlationId);
		if (future != null) {
			try {
				if (getRequiresReply()) {
					future.setException(new JmsTimeoutException("No reply in " + this.receiveTimeout + " ms"));
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Reply expired and reply not required for " + correlationId);
					}
				}
			}
			catch (Exception e) {
				logger.error("Exception while expiring future");
			}
		}
	}

	private void sendRequestMessage(javax.jms.Message jmsRequest, MessageProducer messageProducer, int priority)
			throws JMSException {
		if (this.explicitQosEnabled) {
			messageProducer.send(jmsRequest, this.deliveryMode, priority, this.timeToLive);
		}
		else {
			messageProducer.send(jmsRequest);
		}
	}

	private javax.jms.Message receiveReplyMessage(MessageConsumer messageConsumer) throws JMSException {
		return (this.receiveTimeout >= 0) ? messageConsumer.receive(this.receiveTimeout) : messageConsumer.receive();
	}

	/**
	 * Deletes either a {@link TemporaryQueue} or {@link TemporaryTopic}.
	 * Ignores any other {@link Destination} type and also ignores any
	 * {@link JMSException}s that may be thrown when attempting to delete.
	 */
	private void deleteDestinationIfTemporary(Destination destination) {
		try {
			if (destination instanceof TemporaryQueue) {
				((TemporaryQueue) destination).delete();
			}
			else if (destination instanceof TemporaryTopic) {
				((TemporaryTopic) destination).delete();
			}
		}
		catch (JMSException e) {
			// ignore
		}
	}

	/**
	 * Create a new JMS Connection for this JMS gateway.
	 *
	 * @return The connection.
	 * @throws JMSException Any JMSException.
	 */
	protected Connection createConnection() throws JMSException {
		return this.connectionFactory.createConnection();
	}

	/**
	 * Create a new JMS Session using the provided Connection.
	 *
	 * @param connection The connection.
	 * @return The session.
	 * @throws JMSException Any JMSException.
	 */
	protected Session createSession(Connection connection) throws JMSException {
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	@Override
	public void onMessage(javax.jms.Message message) {
		String correlationId = null;
		try {
			if (logger.isTraceEnabled()) {
				logger.trace(this.getComponentName() + " Received " + message);
			}
			if (this.correlationKey == null ||
					this.correlationKey.equals("JMSCorrelationID") ||
					this.correlationKey.equals("JMSCorrelationID*")) {
				correlationId = message.getJMSCorrelationID();
			}
			else {
				correlationId = message.getStringProperty(this.correlationKey);
			}
			Assert.state(correlationId != null, "Message with no correlationId received");
			if (isAsync()) {
				onMessageAsync(message, correlationId);
			}
			else {
				onMessageSync(message, correlationId);
			}
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to consume reply with correlationId " + correlationId, e);
			}
		}
	}

	private void onMessageAsync(javax.jms.Message message, String correlationId) throws Exception {
		SettableListenableFuture<AbstractIntegrationMessageBuilder<?>> future = this.futures.remove(correlationId);
		if (future != null) {
			message.setJMSCorrelationID(null);
			future.set(buildReply(message));
		}
		else {
			logger.warn("Late reply for " + correlationId);
		}
	}

	private void onMessageSync(javax.jms.Message message, String correlationId) {
		try {
			LinkedBlockingQueue<javax.jms.Message> queue = this.replies.get(correlationId);
			if (queue == null) {
				if (this.correlationKey != null) {
					Log debugLogger = LogFactory.getLog("si.jmsgateway.debug");
					if (debugLogger.isDebugEnabled()) {
						Object siMessage = this.messageConverter.fromMessage(message);
						debugLogger.debug("No pending reply for " + siMessage + " with correlationId: "
								+ correlationId + " pending replies: " + this.replies.keySet());
					}
					throw new RuntimeException("No sender waiting for reply");
				}
				synchronized (this.earlyOrLateReplies) {
					queue = this.replies.get(correlationId);
					if (queue == null) {
						if (logger.isDebugEnabled()) {
							logger.debug("Reply for correlationId " + correlationId + " received early or late");
						}
						this.earlyOrLateReplies.put(correlationId, new TimedReply(message));
					}
				}
			}
			if (queue != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Received reply with correlationId " + correlationId);
				}
				queue.add(message);
			}
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to consume reply with correlationId " + correlationId, e);
			}
		}
	}

	private static class GatewayReplyListenerContainer extends DefaultMessageListenerContainer {

		private volatile Destination replyDestination;

		GatewayReplyListenerContainer() {
			super();
		}

		@Override
		protected Destination resolveDestinationName(Session session, String destinationName) throws JMSException {
			if (!StringUtils.hasText(destinationName)) {
				this.replyDestination = session.createTemporaryQueue();
			}
			else {
				this.replyDestination = super.resolveDestinationName(session, destinationName);
			}
			return this.replyDestination;
		}


		@Override
		protected void validateConfiguration() {
			if (isSubscriptionDurable() && !isPubSubDomain()) {
				throw new IllegalArgumentException("A durable subscription requires a topic (pub-sub domain)");
			}
			synchronized (this.lifecycleMonitor) {
				if (isSubscriptionDurable() && this.getConcurrentConsumers() != 1) {
					throw new IllegalArgumentException("Only 1 concurrent consumer supported for durable subscription");
				}
			}
		}

		public Destination getReplyDestination() {
			Destination replyDest = getDestination();
			if (replyDest == null) {
				replyDest = this.replyDestination;
			}
			if (replyDest != null) {
				return replyDest;
			}
			else {
				int n = 0;
				while (this.replyDestination == null && n++ < 100) {
					logger.debug("Waiting for container to create destination");
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new IllegalStateException("Container did not establish a destination");
					}
				}
				if (this.replyDestination == null) {
					throw new IllegalStateException("Container did not establish a destination");
				}
				else {
					return this.replyDestination;
				}
			}
		}

		@Override
		protected String getDestinationDescription() {
			if (this.replyDestination instanceof TemporaryQueue) {
				return "Temporary queue: " + this.replyDestination.toString();
			}
			else if (super.getDestination() != null) {
				try {
					return super.getDestinationDescription();
				}
				catch (Exception e) {
					if (logger.isWarnEnabled()) {
						logger.warn("Unexpected error obtaining destination description: " + e.getMessage());
					}
					return null;
				}
			}
			else {
				return null;
			}
		}

		@Override
		protected void recoverAfterListenerSetupFailure() {
			if (logger.isDebugEnabled()) {
				logger.debug("recoverAfterListenerSetupFailure for dest: " + this.replyDestination);
			}
			this.replyDestination = null;
			super.recoverAfterListenerSetupFailure();
		}
	}

	private static final class TimedReply {

		private final long timeStamp = System.currentTimeMillis();

		private final javax.jms.Message reply;

		TimedReply(javax.jms.Message reply) {
			this.reply = reply;
		}

		private long getTimeStamp() {
			return this.timeStamp;
		}

		private javax.jms.Message getReply() {
			return this.reply;
		}
	}

	private class LateReplyReaper implements Runnable {

		LateReplyReaper() {
			super();
		}

		@Override
		public void run() {
			if (logger.isTraceEnabled()) {
				logger.trace("Running late reply reaper");
			}
			Iterator<Entry<String, TimedReply>> lateReplyIterator =
					JmsOutboundGateway.this.earlyOrLateReplies.entrySet().iterator();
			long now = System.currentTimeMillis();
			long expired = now - (JmsOutboundGateway.this.receiveTimeout * 2);
			while (lateReplyIterator.hasNext()) {
				Entry<String, TimedReply> entry = lateReplyIterator.next();
				if (entry.getValue().getTimeStamp() < expired) {
					if (logger.isDebugEnabled()) {
						logger.debug("Removing late reply for correlationId " + entry.getKey());
					}
					lateReplyIterator.remove();
				}
			}
			// reschedule myself
			if (JmsOutboundGateway.this.receiveTimeout >= 0) {
				JmsOutboundGateway.this.reaper = getTaskScheduler().schedule(this,
						new Date(now + JmsOutboundGateway.this.receiveTimeout));
			}
		}

	}

	private class IdleContainerStopper implements Runnable {

		IdleContainerStopper() {
			super();
		}

		@Override
		public void run() {
			synchronized (JmsOutboundGateway.this.lifeCycleMonitor) {
				if (System.currentTimeMillis() - JmsOutboundGateway.this.lastSend >
						JmsOutboundGateway.this.idleReplyContainerTimeout
						&& JmsOutboundGateway.this.replies.size() == 0) {
					if (JmsOutboundGateway.this.replyContainer.isRunning()) {
						if (logger.isDebugEnabled()) {
							logger.debug(getComponentName() + ": Stopping idle reply container.");
						}
						JmsOutboundGateway.this.replyContainer.stop();
						JmsOutboundGateway.this.idleTask.cancel(false);
						JmsOutboundGateway.this.idleTask = null;
					}
				}
			}
		}

	}

	public static class ReplyContainerProperties {

		private volatile Boolean sessionTransacted;

		private volatile Integer sessionAcknowledgeMode;

		private volatile String sessionAcknowledgeModeName;

		private volatile Long receiveTimeout;

		private volatile Long recoveryInterval;

		private volatile Integer cacheLevel;

		private volatile Integer concurrentConsumers;

		private volatile Integer maxConcurrentConsumers;

		private volatile Integer maxMessagesPerTask;

		private volatile Integer idleConsumerLimit;

		private volatile Integer idleTaskExecutionLimit;

		private volatile Executor taskExecutor;

		public String getSessionAcknowledgeModeName() {
			return this.sessionAcknowledgeModeName;
		}

		public void setSessionAcknowledgeModeName(String sessionAcknowledgeModeName) {
			this.sessionAcknowledgeModeName = sessionAcknowledgeModeName;
		}

		public Boolean isSessionTransacted() {
			return this.sessionTransacted;
		}

		public void setSessionTransacted(Boolean sessionTransacted) {
			this.sessionTransacted = sessionTransacted;
		}

		public Integer getSessionAcknowledgeMode() {
			return this.sessionAcknowledgeMode;
		}

		public void setSessionAcknowledgeMode(Integer sessionAcknowledgeMode) {
			this.sessionAcknowledgeMode = sessionAcknowledgeMode;
		}

		public Long getReceiveTimeout() {
			return this.receiveTimeout;
		}

		public void setReceiveTimeout(Long receiveTimeout) {
			this.receiveTimeout = receiveTimeout;
		}

		public Long getRecoveryInterval() {
			return this.recoveryInterval;
		}

		public void setRecoveryInterval(Long recoveryInterval) {
			this.recoveryInterval = recoveryInterval;
		}

		public Integer getCacheLevel() {
			return this.cacheLevel;
		}

		public void setCacheLevel(Integer cacheLevel) {
			this.cacheLevel = cacheLevel;
		}

		public Integer getConcurrentConsumers() {
			return this.concurrentConsumers;
		}

		public void setConcurrentConsumers(Integer concurrentConsumers) {
			this.concurrentConsumers = concurrentConsumers;
		}

		public Integer getMaxConcurrentConsumers() {
			return this.maxConcurrentConsumers;
		}

		public void setMaxConcurrentConsumers(Integer maxConcurrentConsumers) {
			this.maxConcurrentConsumers = maxConcurrentConsumers;
		}

		public Integer getMaxMessagesPerTask() {
			return this.maxMessagesPerTask;
		}

		public void setMaxMessagesPerTask(Integer maxMessagesPerTask) {
			this.maxMessagesPerTask = maxMessagesPerTask;
		}

		public Integer getIdleConsumerLimit() {
			return this.idleConsumerLimit;
		}

		public void setIdleConsumerLimit(Integer idleConsumerLimit) {
			this.idleConsumerLimit = idleConsumerLimit;
		}

		public Integer getIdleTaskExecutionLimit() {
			return this.idleTaskExecutionLimit;
		}

		public void setIdleTaskExecutionLimit(Integer idleTaskExecutionLimit) {
			this.idleTaskExecutionLimit = idleTaskExecutionLimit;
		}

		public void setTaskExecutor(Executor taskExecutor) {
			this.taskExecutor = taskExecutor;
		}

		public Executor getTaskExecutor() {
			return this.taskExecutor;
		}

	}

}
