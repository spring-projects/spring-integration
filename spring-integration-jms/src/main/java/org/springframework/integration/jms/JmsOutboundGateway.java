/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * An outbound Messaging Gateway for request/reply JMS.
 *
 * @author Mark Fisher
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Oleg Zhurakousky
 */
public class JmsOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final String gatewayId = UUID.randomUUID().toString();

	private volatile boolean cachedConsumers;

	private volatile Map<Integer, LinkedList<Session>> cachedSessionView;

	private volatile boolean replyDestinationExplicitlySet;

	private final Map<Long, Destination> tempQueuePerSessionMap = new HashMap<Long, Destination>(10);

	private volatile Destination requestDestination;

	private volatile String requestDestinationName;

	private volatile ExpressionEvaluatingMessageProcessor<?> requestDestinationExpressionProcessor;

	private volatile Destination replyDestination;

	private volatile String replyDestinationName;

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

	private final Object initializationMonitor = new Object();


	/**
	 * Set whether message delivery should be persistent or non-persistent,
	 * specified as a boolean value ("true" or "false"). This will set the delivery
	 * mode accordingly to either "PERSISTENT" (1) or "NON_PERSISTENT" (2).
	 * <p>The default is "true", i.e. delivery mode "PERSISTENT".
	 * @see #setDeliveryMode(int)
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 */
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.deliveryMode = (deliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
	}

	/**
	 * Set the JMS ConnectionFactory that this gateway should use.
	 * This is a <em>required</em> property.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Set the JMS Destination to which request Messages should be sent.
	 * Either this or one of 'requestDestinationName' or 'requestDestinationExpression' is required.
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
	 */
	public void setRequestDestinationName(String requestDestinationName) {
		this.requestDestinationName = requestDestinationName;
	}

	/**
	 * Set the SpEL Expression to be used for determining the request Destination instance
	 * or request destination name. Either this or one of 'requestDestination' or
	 * 'requestDestinationName' is required.
	 */
	public void setRequestDestinationExpression(Expression requestDestinationExpression) {
		this.requestDestinationExpressionProcessor = new ExpressionEvaluatingMessageProcessor<Object>(requestDestinationExpression);
	}

	/**
	 * Set the JMS Destination from which reply Messages should be received.
	 * If none is provided, this gateway will create a {@link TemporaryQueue} per invocation.
	 */
	public void setReplyDestination(Destination replyDestination) {
		if (replyDestination instanceof Topic) {
			this.replyPubSubDomain = true;
		}
		this.replyDestination = replyDestination;
		this.replyDestinationExplicitlySet = true;
	}

	/**
	 * Set the name of the JMS Destination from which reply Messages should be received.
	 * If none is provided, this gateway will create a {@link TemporaryQueue} per invocation.
	 */
	public void setReplyDestinationName(String replyDestinationName) {
		this.replyDestinationName = replyDestinationName;
		this.replyDestinationExplicitlySet = true;
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
	 * @param requestPubSubDomain true if the request destination is a Topic
	 */
	public void setRequestPubSubDomain(boolean requestPubSubDomain) {
		this.requestPubSubDomain = requestPubSubDomain;
	}

	/**
	 * Specify whether the reply destination is a Topic. This value is
	 * necessary when providing a destination name for a Topic rather than
	 * a destination reference.
	 *
	 * @param replyPubSubDomain true if the reply destination is a Topic
	 */
	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
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
	 * Specify whether explicit QoS settings are enabled
	 * (deliveryMode, priority, and timeToLive).
	 */
	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.explicitQosEnabled = explicitQosEnabled;
	}

	/**
	 * Provide the name of a JMS property that should hold a generated JMSCorrelationID.
	 * If NO value is provided for this attribute, then the reply consumer's
	 * MessageSelector will be expecting the JMSCorrelationID to equal the Message ID
	 * of the request. However this would also mean that the MessageSelector will be different
	 * for each new message, thus requiring a new reply consumer to be created for each Message sent.
	 * Although this is a very common JMS exchange pattern it is not optimal for high throughput	 request/reply
	 * applications. Most MOMs (including Spring Integration's Inbound Gateway) also support propagation
	 * of the 'JMSCorrelationID' of the consumed request message into the 'JMSCorrelationID' of the newly produced
	 * reply message. This also allows us to use an optimized value generation algorithm which results in caching
	 * and reuse of reply consumers resulting in significant performance improvement of the request/reply
	 * scenarios. So if you have a Spring JMS consumer (e.g.,  Spring Integration's Inbound Gateway) or
	 * you know that your MOM supports propagation of the 'CorrelationID' it is highly recommended to set this
	 * value to 'JMSCorrelationID'. If you set this value to something else the receiving end must now how
	 * to propagate it. For example: if the receiving end is an inbound-gateway its correlation-key attribute
	 * must have the same value.
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
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * Provide a {@link JmsHeaderMapper} implementation for mapping the
	 * Spring Integration Message Headers to/from JMS Message properties.
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
	 * @param extractRequestPayload
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
	 * @param extractReplyPayload
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

	@Override
	public String getComponentType() {
		return "jms:outbound-gateway";
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.connectionFactory, "connectionFactory must not be null");
			Assert.isTrue(this.requestDestination != null
					^ this.requestDestinationName != null
					^ this.requestDestinationExpressionProcessor != null,
					"Exactly one of 'requestDestination', 'requestDestinationName', or 'requestDestinationExpression' is required.");
			super.onInit();
			if (this.requestDestinationExpressionProcessor != null) {
				this.requestDestinationExpressionProcessor.setBeanFactory(getBeanFactory());
				this.requestDestinationExpressionProcessor.setConversionService(getConversionService());
			}
			this.initialized = true;
			if (this.connectionFactory instanceof CachingConnectionFactory){
				this.cachedConsumers = ((CachingConnectionFactory)this.connectionFactory).isCacheConsumers();
				DirectFieldAccessor cfDfa = new DirectFieldAccessor(this.connectionFactory);
				this.cachedSessionView = (Map<Integer, LinkedList<Session>>) cfDfa.getPropertyValue("cachedSessions");
			}

			if (this.cachedConsumers && !StringUtils.hasText(this.correlationKey)
					&& (this.replyDestination != null || StringUtils.hasText(this.replyDestinationName))){
				logger.warn("Caching consumers  without custom correlation-key can lead to " +
						"significant performance degradation and OutOfMemoryError. Either do not use consumer caching " +
						"by setting 'cacheConsumers' attribute to FALSE in the CachingConnectionFactory or set 'correlation-key'" +
						"to a value (e.g., correlation-key=\"JMSCorrelationID\")");
			}
			else if (!this.cachedConsumers && StringUtils.hasText(this.correlationKey)){
				logger.warn("Using custom 'correlationKey' without caching consumers can lead to " +
						"significant performance degradation. Consider using the CachingConnectionFactory " +
						"with 'cacheConsumers' attribute set to TRUE");
			}
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
			Object result = jmsReply;
			if (this.extractReplyPayload) {
				result = this.messageConverter.fromMessage(jmsReply);
				if (logger.isDebugEnabled()) {
					logger.debug("converted JMS Message [" + jmsReply + "] to integration Message payload [" + result + "]");
				}
			}
			Map<String, Object> jmsReplyHeaders = this.headerMapper.toHeaders(jmsReply);
			Message<?> replyMessage = null;
			if (result instanceof Message){
				replyMessage = MessageBuilder.fromMessage((Message<?>) result).copyHeaders(jmsReplyHeaders).build();
			}
			else {
				replyMessage = MessageBuilder.withPayload(result).copyHeaders(jmsReplyHeaders).build();
			}
			return replyMessage;
		}
		catch (JMSException e) {
			throw new MessageHandlingException(requestMessage, e);
		}
	}

	/**
	 * Create a new JMS Connection for this JMS gateway.
	 */
	protected Connection createConnection() throws JMSException {
		return this.connectionFactory.createConnection();
	}

	/**
	 * Create a new JMS Session using the provided Connection.
	 */
	protected Session createSession(Connection connection) throws JMSException {
		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
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
					"Evaluation of requestDestinationExpression failed to produce a Destination or destination name. Result was: " + result);
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

	private Destination determineReplyDestination(Session session, long sessionId) throws JMSException {

		Destination replyDestinationToReturn = this.replyDestination;

		if (replyDestinationToReturn == null){
			if (StringUtils.hasText(this.replyDestinationName)) {
				Assert.notNull(this.destinationResolver,
						"DestinationResolver is required when relying upon the 'replyDestinationName' property.");
				replyDestinationToReturn = this.destinationResolver.resolveDestinationName(
						session, this.replyDestinationName, this.replyPubSubDomain);
			}
			else if (this.tempQueuePerSessionMap.containsKey(sessionId)){
				replyDestinationToReturn = this.tempQueuePerSessionMap.get(sessionId);
			}
			else if (this.cachedConsumers){
				if (StringUtils.hasText(this.correlationKey)){
					// will rely on LIKE  MessageSelector. Only need one TemporaryQueue for reply
					// as if the replyDestinationName was specified
					replyDestinationToReturn = session.createTemporaryQueue();
					this.replyDestination = replyDestinationToReturn;
				}
				else {
					replyDestinationToReturn = session.createTemporaryQueue();
					this.tempQueuePerSessionMap.put(sessionId, replyDestinationToReturn);
				}
			}
			else {
				replyDestinationToReturn = session.createTemporaryQueue();
			}
		}

		return replyDestinationToReturn;
	}

	private javax.jms.Message sendAndReceive(Message<?> requestMessage) throws JMSException {
		Connection connection = this.createConnection();
		Session session = null;
		Destination replyTo = null;
		long sessionId = 0;
		try {
			session = this.createSession(connection);
			sessionId = System.identityHashCode(session);

			// convert to JMS Message
			Object objectToSend = requestMessage;
			if (this.extractRequestPayload) {
				objectToSend = requestMessage.getPayload();
			}
			javax.jms.Message jmsRequest = this.messageConverter.toMessage(objectToSend, session);

			// map headers
			headerMapper.fromHeaders(requestMessage.getHeaders(), jmsRequest);

			// TODO: support a JmsReplyTo header in the SI Message?
			replyTo = this.determineReplyDestination(session, sessionId);

			connection.start();

			Integer priority = requestMessage.getHeaders().getPriority();
			if (priority == null) {
				priority = this.priority;
			}

			Destination requestDestinationToUse = this.determineRequestDestination(requestMessage, session);

			javax.jms.Message replyMessage = this.doSendAndReceive(requestDestinationToUse, jmsRequest, replyTo, session, priority, sessionId);

			return replyMessage;
		}
		finally {
			JmsUtils.closeSession(session);

			if (!this.replyDestinationExplicitlySet &&
				this.isTemporaryDestination(replyTo) &&
				!this.isCachedSession(session)) {

				this.deleteDestinationIfTemporary(replyTo);
				this.tempQueuePerSessionMap.remove(sessionId);
			}

			ConnectionFactoryUtils.releaseConnection(connection, this.connectionFactory, true);
		}
	}

	/**
	 * Creates the MessageConsumer after sending the request Message since we need the MessageID for correlation with a MessageSelector.
	 */
	private javax.jms.Message doSendAndReceive(Destination requestDestinationToUse,
			javax.jms.Message jmsRequest, Destination replyTo, Session session, int priority, long sessionId) throws JMSException {

		MessageProducer messageProducer = session.createProducer(requestDestinationToUse);
		jmsRequest.setJMSReplyTo(replyTo);
		try {
			if (StringUtils.hasText(this.correlationKey)){
				return this.exchangeWithCorrelationKey(messageProducer, jmsRequest, session, sessionId, replyTo, priority);
			}
			else {
				return this.exchangeWithoutCorrelationKey(messageProducer, jmsRequest, session, sessionId, replyTo, priority);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(messageProducer);
		}
	}


	private javax.jms.Message exchangeWithCorrelationKey(MessageProducer messageProducer, javax.jms.Message jmsRequest,
			Session session, long sessionId, Destination replyTo, int priority) throws JMSException {

		String messageCorrelationId = ObjectUtils.getIdentityHexString(jmsRequest);

		String consumerCorrelationId = gatewayId + "_" + sessionId;

		if (this.correlationKey.equals("JMSCorrelationID")) {
			jmsRequest.setJMSCorrelationID(consumerCorrelationId + "$" + messageCorrelationId);
		}
		else {
			jmsRequest.setStringProperty(correlationKey, consumerCorrelationId + "$" + messageCorrelationId);
		}

		String messageSelector = correlationKey + " LIKE '" + consumerCorrelationId + "%'";

		MessageConsumer messageConsumer = null;

		try {
			messageConsumer = this.createMessageConsumer(session, replyTo, messageSelector, sessionId);

			this.sendRequestMessage(jmsRequest, messageProducer, priority);

			return this.doReceive(messageConsumer, this.correlationKey, messageCorrelationId, sessionId);
		}
		finally  {
			JmsUtils.closeMessageConsumer(messageConsumer);
		}
	}

	private javax.jms.Message exchangeWithoutCorrelationKey(MessageProducer messageProducer, javax.jms.Message jmsRequest,
			Session session, long sessionId, Destination replyTo, int priority) throws JMSException {

		MessageConsumer messageConsumer = null;
		try {
			String messageCorrelationId = null;

			if (this.replyDestinationExplicitlySet){
				if (replyTo instanceof Topic && logger.isWarnEnabled()) {
					logger.warn("Relying on the MessageID for correlation is not recommended when using a Topic as the replyTo Destination " +
							"because that ID can only be provided to a MessageSelector after the request Message has been sent thereby " +
							"creating a race condition where a fast response might be sent before the MessageConsumer has been created. " +
							"Consider providing a value to the 'correlationKey' property of this gateway instead. Then the MessageConsumer " +
							"will be created before the request Message is sent.");
				}
				this.sendRequestMessage(jmsRequest, messageProducer, priority);
				messageCorrelationId = jmsRequest.getJMSMessageID().replaceAll("'", "''");
				String messageSelector = "JMSCorrelationID = '" + messageCorrelationId + "'";
				messageConsumer = this.createMessageConsumer(session, replyTo, messageSelector, sessionId);
			}
			else {
				messageConsumer = this.createMessageConsumer(session, replyTo, null, sessionId);
				this.sendRequestMessage(jmsRequest, messageProducer, priority);
				messageCorrelationId = jmsRequest.getJMSMessageID().replaceAll("'", "''");
			}

			return this.doReceive(messageConsumer, null, messageCorrelationId, sessionId);
		}
		finally  {
			JmsUtils.closeMessageConsumer(messageConsumer);
		}
	}

	/**
	 * Will perform clean up if failures detected during message receive
	 */
	private javax.jms.Message doReceive(MessageConsumer messageConsumer, String correlationKey, String messageCorrelationIdToMatch, long sessionId) throws JMSException{
		try {
			return this.receiveCorrelatedReplyMessage(messageConsumer, correlationKey, messageCorrelationIdToMatch);
		}
		catch (javax.jms.IllegalStateException e) {
			this.clearDestination(sessionId);
			throw e;
		}
	}

	private javax.jms.Message receiveCorrelatedReplyMessage(MessageConsumer messageConsumer,
			String correlationKey, String messageCorrelationIdToMatch) throws JMSException {

		long timeout = this.receiveTimeout;
		long startTime = System.currentTimeMillis();
		javax.jms.Message replyMessage = (this.receiveTimeout >= 0) ? messageConsumer.receive(receiveTimeout) : messageConsumer.receive();

		while (replyMessage != null){

			String jmsCorrelationId = null;
			if (correlationKey != null && !correlationKey.equals("JMSCorrelationID")){
				jmsCorrelationId = replyMessage.getStringProperty(correlationKey);
			}
			else {
				jmsCorrelationId = replyMessage.getJMSCorrelationID();
			}

			if (StringUtils.hasText(jmsCorrelationId) && StringUtils.hasText(messageCorrelationIdToMatch)){

				if (jmsCorrelationId.contains(messageCorrelationIdToMatch)){
					return replyMessage;
				}
				else {
					// Essentially we are discarding the uncorrelated message here and moving on to
					// the next one since we can no longer communicate with its originating producer
					// since it has already timed out waiting for the reply. We are also honoring the original timeout
					if (this.logger.isDebugEnabled()){
						this.logger.debug("Discarded late arriving reply: " + replyMessage);
					}
					if (timeout > 0){
						long elapsedTime = System.currentTimeMillis() - startTime;
						timeout = timeout - elapsedTime;
						if (timeout < 0){
							return null;
						}
						replyMessage = messageConsumer.receive(timeout);
					}
					else if (this.receiveTimeout < 0){
						replyMessage = messageConsumer.receive();
					}
					else {
						return null;
					}
				}
			}
			else {
				return replyMessage;
			}
		}

		return null;
	}

	private MessageConsumer createMessageConsumer(Session session, Destination replyTo, String messageSelector, long sessionId) throws JMSException{
		try {
			if (this.cachedConsumers && this.isTemporaryDestination(replyTo)){
				if (StringUtils.hasText(messageSelector)){
					return TemporaryConsumerUtils.createConsumer(session, replyTo, messageSelector);
				}
				else {
					return TemporaryConsumerUtils.createConsumer(session, replyTo, null);
				}
			}
			else {
				if (StringUtils.hasText(messageSelector)){
					return session.createConsumer(replyTo, messageSelector);
				}
				else {
					return session.createConsumer(replyTo);
				}
			}
		}
		catch (InvalidDestinationException e) {
			if (this.tempQueuePerSessionMap.containsKey(sessionId)){
				this.tempQueuePerSessionMap.remove(sessionId);
			}
			else if (this.isTemporaryDestination(this.replyDestination) && !this.replyDestinationExplicitlySet){
				this.replyDestination = null;
			}
			throw e;
		}
	}

	private void sendRequestMessage(javax.jms.Message jmsRequest, MessageProducer messageProducer, int priority) throws JMSException {
		if (this.explicitQosEnabled) {
			messageProducer.send(jmsRequest, this.deliveryMode, priority, this.timeToLive);
		}
		else {
			messageProducer.send(jmsRequest);
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
	 * Will clear reply destination *only* if such reply destination is temporary
	 * and was created internally
	 */
	private void clearDestination(long sessionId){
		if (this.tempQueuePerSessionMap.containsKey(sessionId)){
			this.tempQueuePerSessionMap.remove(sessionId);
		}
		else if (this.isTemporaryDestination(this.replyDestination) && !this.replyDestinationExplicitlySet){
			this.replyDestination = null;
		}
	}

	/**
	 * Checks if this destination (topic or queue) is a temporary destination
	 */
	private boolean isTemporaryDestination(Destination destination){
		return destination instanceof TemporaryQueue || destination instanceof TemporaryTopic;
	}

	/**
	 * Validates if this session is cached by the CachingConnectionFactory
	 */
	private boolean isCachedSession(Session session){
		try {
			if (this.cachedSessionView.containsKey(Session.AUTO_ACKNOWLEDGE)){
				List<Session> cachedSessions = this.cachedSessionView.get(Session.AUTO_ACKNOWLEDGE);
				if (cachedSessions != null){
					return cachedSessions.contains(session);
				}
			}
		} catch (NullPointerException e) {
			// ignore
		}
		return false;
	}
}
