/*
 * Copyright 2002-2009 the original author or authors.
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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageTimeoutException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.Assert;

/**
 * An outbound Messaging Gateway for request/reply JMS.
 * 
 * @author Mark Fisher
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class JmsOutboundGateway extends AbstractReplyProducingMessageHandler implements InitializingBean {

	private volatile Destination requestDestination;

	private volatile String requestDestinationName;

	private volatile Destination replyDestination;

	private volatile String replyDestinationName;

	private volatile DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private volatile boolean pubSubDomain;

	private volatile long receiveTimeout = 5000;

	private volatile int deliveryMode = javax.jms.Message.DEFAULT_DELIVERY_MODE;

	private volatile long timeToLive = javax.jms.Message.DEFAULT_TIME_TO_LIVE;

	private volatile int priority = javax.jms.Message.DEFAULT_PRIORITY;

	private ConnectionFactory connectionFactory;

	private volatile MessageConverter messageConverter;

	private volatile JmsHeaderMapper headerMapper;

	private volatile boolean extractRequestPayload = true;

	private volatile boolean extractReplyPayload = true;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	/**
	 * Set the JMS ConnectionFactory that this gateway should use.
	 * This is a <em>required</em> property.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Set the JMS Destination to which request Messages should be sent.
	 * Either this or the 'requestDestinationName' property is required.
	 */
	public void setRequestDestination(Destination requestDestination) {
		if (requestDestination instanceof Topic) {
			this.pubSubDomain = true;
		}
		this.requestDestination = requestDestination;
	}

	/**
	 * Set the name of the JMS Destination to which request Messages should be
	 * sent. Either this or the 'requestDestination' property is required.
	 */
	public void setRequestDestinationName(String requestDestinationName) {
		this.requestDestinationName = requestDestinationName;
	}

	/**
	 * Set the JMS Destination from which reply Messages should be received.
	 * If none is provided, this gateway will create a {@link TemporaryQueue} per invocation.
	 */
	public void setReplyDestination(Destination replyDestination) {
		this.replyDestination = replyDestination;
	}

	/**
	 * Set the name of the JMS Destination from which reply Messages should be received.
	 * If none is provided, this gateway will create a {@link TemporaryQueue} per invocation.
	 */
	public void setReplyDestinationName(String replyDestinationName) {
		this.replyDestinationName = replyDestinationName;
	}

	/**
	 * Provide the {@link DestinationResolver} to use when resolving either a
	 * 'requestDestinationName' or 'replyDestinationName' value. The default
	 * is an instance of {@link DynamicDestinationResolver}.
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Specify whether the request destination is a Topic. This value is
	 * necessary when providing a destination name for a Topic rather than
	 * a destination reference.
	 * 
	 * @param pubSubDomain true if the request destination is a Topic
	 */
	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	/**
	 * Set the max timeout value for the MessageConsumer's receive call when
	 * waiting for a reply. The default value is 5 seconds.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Specify the JMS DeliveryMode to use when sending request Messages.
	 */
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * Specify the JMS priority to use when sending request Messages.
	 * The value should be within the range of 0-9.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Specify the timeToLive for each sent Message.
	 * The default value indicates no expiration.
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * Provide a {@link MessageConverter} strategy to use for converting the
	 * Spring Integration request Message into a JMS Message and for converting
	 * the JMS reply Messages back into Spring Integration Messages.
	 * <p>
	 * The default is a {@link HeaderMappingMessageConverter} that delegates to
	 * a {@link SimpleMessageConverter}.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * Provide a {@link JmsHeaderMapper} implementation for mapping the
	 * Spring Integration Message Headers to/from JMS Message properties.
	 * 
	 * <p>This property will be ignored if a {@link MessageConverter} is
	 * provided to the {@link #setMessageConverter(MessageConverter)} method.
	 * However, you may provide your own implementation of the delegating
	 * {@link HeaderMappingMessageConverter} implementation.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * This property will take effect if no custom {@link MessageConverter}
	 * has been provided to the {@link #setMessageConverter(MessageConverter)}
	 * method. In that case, a {@link HeaderMappingMessageConverter} will be
	 * used by default, and this value will be passed along to that converter's
	 * 'extractIntegrationMessagePayload' property.
	 * 
	 * @see HeaderMappingMessageConverter#setExtractIntegrationMessagePayload(boolean)
	 */
	public void setExtractRequestPayload(boolean extractRequestPayload) {
		this.extractRequestPayload = extractRequestPayload;
	}

	/**
	 * This property will take effect if no custom {@link MessageConverter}
	 * has been provided to the {@link #setMessageConverter(MessageConverter)}
	 * method. In that case, a {@link HeaderMappingMessageConverter} will be
	 * used by default, and this value will be passed along to that converter's
	 * 'extractJmsMessageBody' property.
	 * 
	 * @see HeaderMappingMessageConverter#setExtractJmsMessageBody(boolean)
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	/**
	 * Specify the Spring Integration reply channel. If this property is not
	 * set the gateway will check for a 'replyChannel' header on the request.
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	private Destination getRequestDestination(Session session) throws JMSException {
		if (this.requestDestination != null) {
			return this.requestDestination;
		}
		Assert.notNull(this.destinationResolver,
				"DestinationResolver is required when relying upon the 'requestDestinationName' property.");
		return this.destinationResolver.resolveDestinationName(
				session, this.requestDestinationName, this.pubSubDomain);
	}

	private Destination getReplyDestination(Session session) throws JMSException {
		if (this.replyDestination != null) {
			return this.replyDestination;
		}
		if (this.replyDestinationName != null) {
			Assert.notNull(this.destinationResolver,
					"DestinationResolver is required when relying upon the 'replyDestinationName' property.");
			return this.destinationResolver.resolveDestinationName(
					session, this.replyDestinationName, this.pubSubDomain);
		}
		return session.createTemporaryQueue();
	}

	public void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.connectionFactory, "connectionFactory must not be null");
			Assert.isTrue(this.requestDestination != null || this.requestDestinationName != null,
					"Either a 'requestDestination' or 'requestDestinationName' is required.");
			if (this.messageConverter == null) {
				HeaderMappingMessageConverter hmmc = new HeaderMappingMessageConverter(null, this.headerMapper);
				hmmc.setExtractIntegrationMessagePayload(this.extractRequestPayload);
				hmmc.setExtractJmsMessageBody(this.extractReplyPayload);
				this.messageConverter = hmmc;
			}
			this.initialized = true;
		}
	}

	@Override
	protected Object handleRequestMessage(final Message<?> message) {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		final Message<?> requestMessage = MessageBuilder.fromMessage(message).build();
		try {
			javax.jms.Message jmsReply = this.sendAndReceive(requestMessage);
			if (jmsReply == null) {
				throw new MessageTimeoutException(message,
						"failed to receive JMS response within timeout of: " + this.receiveTimeout + "ms");
			}
			return this.messageConverter.fromMessage(jmsReply);
		}
		catch (JMSException e) {
			throw new MessageHandlingException(requestMessage, e);
		}
	}

	private javax.jms.Message sendAndReceive(Message<?> requestMessage) throws JMSException {
		Connection connection = createConnection();
		Session session = null;
		MessageProducer messageProducer = null;
		MessageConsumer messageConsumer = null;
		Destination replyTo = null;
		try {
			session = createSession(connection);
			javax.jms.Message jmsRequest = this.messageConverter.toMessage(requestMessage, session);
			messageProducer = session.createProducer(this.getRequestDestination(session));
			messageProducer.setDeliveryMode(this.deliveryMode);
			messageProducer.setPriority(this.priority);
			messageProducer.setTimeToLive(this.timeToLive);
			replyTo = this.getReplyDestination(session);
			jmsRequest.setJMSReplyTo(replyTo);
			connection.start();
			messageProducer.send(jmsRequest);
			if (replyTo instanceof TemporaryQueue || replyTo instanceof TemporaryTopic) {
				messageConsumer = session.createConsumer(replyTo);
			}
			else {
				String messageId = jmsRequest.getJMSMessageID().replaceAll("'", "''");
				String messageSelector = "JMSCorrelationID = '" + messageId + "'";
				messageConsumer = session.createConsumer(replyTo, messageSelector);
			}
			return (this.receiveTimeout >= 0) ? messageConsumer.receive(receiveTimeout) : messageConsumer.receive();
		}
		finally {
			JmsUtils.closeMessageProducer(messageProducer);
			JmsUtils.closeMessageConsumer(messageConsumer);
			JmsUtils.closeSession(session);
			this.deleteDestinationIfTemporary(replyTo);
			ConnectionFactoryUtils.releaseConnection(connection, this.connectionFactory, true);
		}
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
	 * Create a new JMS Connection for this JMS gateway, ideally a
	 * <code>javax.jms.QueueConnection</code>.
	 * <p>The default implementation uses the
	 * <code>javax.jms.QueueConnectionFactory</code> API if available,
	 * falling back to a standard JMS 1.1 ConnectionFactory otherwise.
	 * This is necessary for working with generic JMS 1.1 connection pools
	 * (such as ActiveMQ's <code>org.apache.activemq.pool.PooledConnectionFactory</code>).
	 */
	protected Connection createConnection() throws JMSException {
		ConnectionFactory cf = this.connectionFactory;
		if (!this.pubSubDomain && cf instanceof QueueConnectionFactory) {
			return ((QueueConnectionFactory) cf).createQueueConnection();
		}
		return cf.createConnection();
	}

	/**
	 * Create a new JMS Session for this JMS gateway, ideally a
	 * <code>javax.jms.QueueSession</code>.
	 * <p>The default implementation uses the
	 * <code>javax.jms.QueueConnection</code> API if available,
	 * falling back to a standard JMS 1.1 Connection otherwise.
	 * This is necessary for working with generic JMS 1.1 connection pools
	 * (such as ActiveMQ's <code>org.apache.activemq.pool.PooledConnectionFactory</code>).
	 */
	protected Session createSession(Connection connection) throws JMSException {
		if (!this.pubSubDomain && connection instanceof QueueConnection) {
			return ((QueueConnection) connection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		}
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

}
