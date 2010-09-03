/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.Assert;

/**
 * JMS MessageListener that converts a JMS Message into a Spring Integration
 * Message and sends that Message to a channel. If the 'expectReply' value is
 * <code>true</code>, it will also wait for a Spring Integration reply Message
 * and convert that into a JMS reply.
 * 
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Oleg Zhurakousky
 */
public class ChannelPublishingJmsMessageListener extends MessagingGatewaySupport
		implements SessionAwareMessageListener<javax.jms.Message>, InitializingBean {
	
	private volatile boolean expectReply;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile boolean extractRequestPayload = true;

	private volatile boolean extractReplyPayload = true;

	private volatile Object defaultReplyDestination;

	private volatile long replyTimeToLive = javax.jms.Message.DEFAULT_TIME_TO_LIVE;

	private volatile int replyPriority = javax.jms.Message.DEFAULT_PRIORITY;

	private volatile int replyDeliveryMode = javax.jms.Message.DEFAULT_DELIVERY_MODE;

	private volatile boolean explicitQosEnabledForReplies;

	private volatile DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private volatile JmsHeaderMapper headerMapper = new DefaultJmsHeaderMapper();
	
	public String getComponentType(){
		return "jms:inbound-gateway";
	}

	/**
	 * Specify whether a JMS reply Message is expected.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}
	/**
	 * Set the default reply destination to send reply messages to. This will
	 * be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 */
	public void setDefaultReplyDestination(Destination defaultReplyDestination) {
		this.defaultReplyDestination = defaultReplyDestination;
	}

	/**
	 * Set the name of the default reply queue to send reply messages to.
	 * This will be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 * <p>Alternatively, specify a JMS Destination object as "defaultReplyDestination".
	 * @see #setDestinationResolver
	 * @see #setDefaultReplyDestination(javax.jms.Destination)
	 */
	public void setDefaultReplyQueueName(String destinationName) {
		this.defaultReplyDestination = new DestinationNameHolder(destinationName, false);
	}

	/**
	 * Set the name of the default reply topic to send reply messages to.
	 * This will be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 * <p>Alternatively, specify a JMS Destination object as "defaultReplyDestination".
	 * @see #setDestinationResolver
	 * @see #setDefaultReplyDestination(javax.jms.Destination)
	 */
	public void setDefaultReplyTopicName(String destinationName) {
		this.defaultReplyDestination = new DestinationNameHolder(destinationName, true);
	}

	/**
	 * Specify the time-to-live property for JMS reply Messages.
	 * @see javax.jms.MessageProducer#setTimeToLive(long)
	 */
	public void setReplyTimeToLive(long replyTimeToLive) {
		this.replyTimeToLive = replyTimeToLive;
	}

	/**
	 * Specify the priority value for JMS reply Messages.
	 * @see javax.jms.MessageProducer#setPriority(int)
	 */
	public void setReplyPriority(int replyPriority) {
		this.replyPriority = replyPriority;
	}

	/**
	 * Specify the delivery mode for JMS reply Messages.
	 * @see javax.jms.MessageProducer#setDeliveryMode(int)
	 */
	public void setReplyDeliveryPersistent(boolean replyDeliveryPersistent) {
		this.replyDeliveryMode = replyDeliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
	}

	/**
	 * Specify whether explicit QoS should be enabled for replies
	 * (for timeToLive, priority, and deliveryMode settings). 
	 */
	public void setExplicitQosEnabledForReplies(boolean explicitQosEnabledForReplies) {
		this.explicitQosEnabledForReplies = explicitQosEnabledForReplies;
	}

	/**
	 * Set the DestinationResolver that should be used to resolve reply
	 * destination names for this listener.
	 * <p>The default resolver is a DynamicDestinationResolver. Specify a
	 * JndiDestinationResolver for resolving destination names as JNDI locations.
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "destinationResolver must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Provide a {@link MessageConverter} implementation to use when
	 * converting between JMS Messages and Spring Integration Messages.
	 * If none is provided, a {@link DefaultMessageConverter} will
	 * be used and the {@link JmsHeaderMapper} instance provided to the
	 * {@link #setHeaderMapper(JmsHeaderMapper)} method will be included
	 * in the conversion process.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Provide a {@link JmsHeaderMapper} implementation to use when
	 * converting between JMS Messages and Spring Integration Messages.
	 * If none is provided, a {@link DefaultJmsHeaderMapper} will be used.
	 * 
	 * <p>This property will be ignored if a {@link MessageConverter} is
	 * provided to the {@link #setMessageConverter(MessageConverter)} method.
	 * However, you may provide your own implementation of the delegating
	 * {@link DefaultMessageConverter} implementation.
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify whether the JMS request Message's body should be extracted prior
	 * to converting into a Spring Integration Message. This value is set to
	 * <code>true</code> by default. To send the JMS Message itself as a
	 * Spring Integration Message payload, set this to <code>false</code>.
	 */
	public void setExtractRequestPayload(boolean extractRequestPayload) {
		this.extractRequestPayload = extractRequestPayload;
	}

	/**
	 * Specify whether the Spring Integration reply Message's payload should be
	 * extracted prior to converting into a JMS Message. This value is set to
	 * <code>true</code> by default. To send the Spring Integration Message
	 * itself as the JMS Message's body, set this to <code>false</code>.
	 */
	public void setExtractReplyPayload(boolean extractReplyPayload) {
		this.extractReplyPayload = extractReplyPayload;
	}

	@SuppressWarnings("unchecked")
	public void onMessage(javax.jms.Message jmsMessage, Session session) throws JMSException {
		Object result = jmsMessage;
		if (this.extractRequestPayload) {
			result = this.messageConverter.fromMessage(jmsMessage);
			if (logger.isDebugEnabled()) {
				logger.debug("converted JMS Message [" + jmsMessage + "] to integration Message payload [" + result + "]");
			}
		}
	
		Map<String, Object> headers = (Map<String, Object>) headerMapper.toHeaders(jmsMessage);
		Message<?> requestMessage = (result instanceof Message<?>) ?
				MessageBuilder.fromMessage((Message<?>) result).copyHeaders(headers).build() : 
				MessageBuilder.withPayload(result).copyHeaders(headers).build();
		if (!this.expectReply) {
			this.send(requestMessage);
		}
		else {
			Message<?> replyMessage = this.sendAndReceiveMessage(requestMessage);
			if (replyMessage != null) {
				Destination destination = this.getReplyDestination(jmsMessage, session);
				if (destination != null){
					// convert SI Message to JMS Message
					Object replyResult = replyMessage;
					if (this.extractReplyPayload){
						replyResult = replyMessage.getPayload();
					}
					javax.jms.Message jmsReply = this.messageConverter.toMessage(replyResult, session);
					// map SI Message Headers to JMS Message Properties/Headers
					headerMapper.fromHeaders(replyMessage.getHeaders(), jmsReply);
					
					if (jmsReply.getJMSCorrelationID() == null) {
						jmsReply.setJMSCorrelationID(jmsMessage.getJMSMessageID());
					}
					MessageProducer producer = session.createProducer(destination);
					try {
						if (this.explicitQosEnabledForReplies) {
							producer.send(jmsReply,
									this.replyDeliveryMode, this.replyPriority, this.replyTimeToLive);
						}
						else {
							producer.send(jmsReply);
						}
					}
					finally {
						producer.close();
					}
				}
			}
		}
	}

	/**
	 * Determine a reply destination for the given message.
	 * <p>
	 * This implementation first checks the boolean 'error' flag which signifies that the reply is an error message. If
	 * reply is not an error it will first check the JMS Reply-To {@link Destination} of the supplied request message;
	 * if that is not <code>null</code> it is returned; if it is <code>null</code>, then the configured
	 * {@link #resolveDefaultReplyDestination default reply destination} is returned; if this too is <code>null</code>,
	 * then an {@link InvalidDestinationException} is thrown.
	 * @param request the original incoming JMS message
	 * @param session the JMS Session to operate on
	 * @return the reply destination (never <code>null</code>)
	 * @throws JMSException if thrown by JMS API methods
	 * @throws InvalidDestinationException if no {@link Destination} can be determined
	 * @see #setDefaultReplyDestination
	 * @see javax.jms.Message#getJMSReplyTo()
	 */
	private Destination getReplyDestination(javax.jms.Message request, Session session) throws JMSException {
		Destination replyTo = request.getJMSReplyTo();
		if (replyTo == null) {
			replyTo = resolveDefaultReplyDestination(session);
			if (replyTo == null) {
				throw new InvalidDestinationException("Cannot determine reply destination: " +
						"Request message does not contain reply-to destination, and no default reply destination set.");
			}
		}
		return replyTo;
	}

	/**
	 * Resolve the default reply destination into a JMS {@link Destination}, using this
	 * listener's {@link DestinationResolver} in case of a destination name.
	 * @return the located {@link Destination}
	 * @throws javax.jms.JMSException if resolution failed
	 * @see #setDefaultReplyDestination
	 * @see #setDefaultReplyQueueName
	 * @see #setDefaultReplyTopicName
	 * @see #setDestinationResolver
	 */
	private Destination resolveDefaultReplyDestination(Session session) throws JMSException {
		if (this.defaultReplyDestination instanceof Destination) {
			return (Destination) this.defaultReplyDestination;
		}
		if (this.defaultReplyDestination instanceof DestinationNameHolder) {
			DestinationNameHolder nameHolder = (DestinationNameHolder) this.defaultReplyDestination;
			return this.destinationResolver.resolveDestinationName(session, nameHolder.name, nameHolder.isTopic);
		}
		return null;
	}


	/**
	 * Internal class combining a destination name
	 * and its target destination type (queue or topic).
	 */
	private static class DestinationNameHolder {

		public final String name;

		public final boolean isTopic;

		public DestinationNameHolder(String name, boolean isTopic) {
			this.name = name;
			this.isTopic = isTopic;
		}
	}
}
