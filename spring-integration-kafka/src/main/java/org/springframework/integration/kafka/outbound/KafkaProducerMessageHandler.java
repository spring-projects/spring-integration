/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.integration.kafka.outbound;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.kafka.support.KafkaIntegrationHeaders;
import org.springframework.integration.kafka.support.KafkaSendFailureException;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.kafka.support.JacksonPresent;
import org.springframework.kafka.support.KafkaHeaderMapper;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.SimpleKafkaHeaderMapper;
import org.springframework.kafka.support.converter.KafkaMessageHeaders;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A Message Handler for Apache Kafka; when supplied with a {@link ReplyingKafkaTemplate} it is used as
 * the handler in an outbound gateway. When supplied with a simple {@link KafkaTemplate}
 * it used as the handler in an outbound channel adapter.
 * <p>
 * The handler also supports receiving a pre-built
 * {@link ProducerRecord} payload. In that case, most configuration properties
 * ({@link #setTopicExpression(Expression)} etc.) are ignored. If the handler is used as
 * gateway, the {@link ProducerRecord} will have its headers enhanced to add the
 * {@link KafkaHeaders#REPLY_TOPIC} unless it already contains such a header. The handler
 * will not map any additional headers; providing such a payload assumes the headers have
 * already been mapped.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 *
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Gary Russell
 * @author Marius Bogoevici
 * @author Biju Kunjummen
 * @author Tom van den Berge
 *
 * @since 5.4
 */
public class KafkaProducerMessageHandler<K, V> extends AbstractReplyProducingMessageHandler
		implements ManageableLifecycle {

	/**
	 * Buffer added to ensure our timeout is longer than Apache Kafka timeout.
	 */
	private static final int DEFAULT_TIMEOUT_BUFFER = 5000;

	private static final int TWENTY = 20;

	private static final Duration DEFAULT_ASSIGNMENT_TIMEOUT = Duration.ofSeconds(TWENTY);

	private final Map<String, Set<Integer>> replyTopicsAndPartitions = new HashMap<>();

	private final KafkaTemplate<K, V> kafkaTemplate;

	private final boolean isGateway;

	private final boolean transactional;

	private final boolean allowNonTransactional;

	private final AtomicBoolean running = new AtomicBoolean();

	private final long deliveryTimeoutMsProperty;

	private EvaluationContext evaluationContext;

	private Expression topicExpression;

	private Expression messageKeyExpression;

	private Expression partitionIdExpression;

	private Expression timestampExpression;

	private Expression flushExpression = new FunctionExpression<Message<?>>(message ->
			Boolean.TRUE.equals(message.getHeaders().get(KafkaIntegrationHeaders.FLUSH)));

	private boolean sync;

	private Expression sendTimeoutExpression;

	private KafkaHeaderMapper headerMapper;

	private RecordMessageConverter replyMessageConverter = new MessagingMessageConverter();

	private MessageChannel sendFailureChannel;

	private String sendFailureChannelName;

	private MessageChannel sendSuccessChannel;

	private String sendSuccessChannelName;

	private MessageChannel futuresChannel;

	private String futuresChannelName;

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	private Type replyPayloadType = Object.class;

	private ProducerRecordCreator<K, V> producerRecordCreator =
			(message, topic, partition, timestamp, key, value, headers) ->
					new ProducerRecord<>(topic, partition, timestamp, key, value, headers);

	private int timeoutBuffer = DEFAULT_TIMEOUT_BUFFER;

	private boolean useTemplateConverter;

	private Duration assignmentDuration = DEFAULT_ASSIGNMENT_TIMEOUT;

	private volatile byte[] singleReplyTopic;

	public KafkaProducerMessageHandler(final KafkaTemplate<K, V> kafkaTemplate) {
		Assert.notNull(kafkaTemplate, "kafkaTemplate cannot be null");
		this.kafkaTemplate = kafkaTemplate;
		this.isGateway = kafkaTemplate instanceof ReplyingKafkaTemplate;
		if (this.isGateway) {
			setAsync(true);
			updateNotPropagatedHeaders(
					new String[] {KafkaHeaders.TOPIC, KafkaHeaders.PARTITION, KafkaHeaders.KEY}, false);
		}
		if (JacksonPresent.isJackson2Present()) {
			this.headerMapper = new DefaultKafkaHeaderMapper();
		}
		else {
			this.headerMapper = new SimpleKafkaHeaderMapper();
		}
		this.transactional = kafkaTemplate.isTransactional();
		this.allowNonTransactional = kafkaTemplate.isAllowNonTransactional();
		if (this.transactional && this.isGateway) {
			logger.warn("The KafkaTemplate is transactional; this gateway will only work if the consumer is "
					+ "configured to read uncommitted records");
		}
		determineSendTimeout();
		this.deliveryTimeoutMsProperty =
				this.sendTimeoutExpression.getValue(Long.class) // NOSONAR - never null after determineSendTimeout()
						- this.timeoutBuffer;
	}

	private void determineSendTimeout() {
		Map<String, Object> props = this.kafkaTemplate.getProducerFactory().getConfigurationProperties();
		Object dt = props.get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG);
		if (dt == null) {
			dt = ProducerConfig.configDef().defaultValues().get(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG);
		}
		if (dt instanceof Long) {
			setSendTimeout(((Long) dt) + this.timeoutBuffer);
		}
		else if (dt instanceof Integer) {
			setSendTimeout(Long.valueOf((Integer) dt) + this.timeoutBuffer);
		}
		else if (dt instanceof String) {
			setSendTimeout(Long.parseLong((String) dt) + this.timeoutBuffer);
		}
	}

	public void setTopicExpression(Expression topicExpression) {
		this.topicExpression = topicExpression;
	}

	public void setMessageKeyExpression(Expression messageKeyExpression) {
		this.messageKeyExpression = messageKeyExpression;
	}

	public void setPartitionIdExpression(Expression partitionIdExpression) {
		this.partitionIdExpression = partitionIdExpression;
	}

	/**
	 * Specify a SpEL expression to evaluate a timestamp that will be added in the Kafka record.
	 * The resulting value should be a {@link Long} type representing epoch time in milliseconds.
	 * @param timestampExpression the {@link Expression} for timestamp to wait for result
	 * fo send operation.
	 */
	public void setTimestampExpression(Expression timestampExpression) {
		this.timestampExpression = timestampExpression;
	}

	/**
	 * Specify a SpEL expression that evaluates to a {@link Boolean} to determine whether
	 * the producer should be flushed after the send. Defaults to looking for a
	 * {@link Boolean} value in a {@link KafkaIntegrationHeaders#FLUSH} header; false if
	 * absent.
	 * @param flushExpression the {@link Expression}.
	 */
	public void setFlushExpression(Expression flushExpression) {
		Assert.notNull(flushExpression, "'flushExpression' cannot be null");
		this.flushExpression = flushExpression;
	}

	/**
	 * Set the header mapper to use.
	 * @param headerMapper the mapper; can be null to disable header mapping.
	 */
	public void setHeaderMapper(KafkaHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	public KafkaHeaderMapper getHeaderMapper() {
		return this.headerMapper;
	}

	public KafkaTemplate<?, ?> getKafkaTemplate() {
		return this.kafkaTemplate;
	}

	/**
	 * A {@code boolean} indicating if the {@link KafkaProducerMessageHandler}
	 * should wait for the send operation results or not. Defaults to {@code false}.
	 * In {@code sync} mode a downstream send operation exception will be re-thrown.
	 * @param sync the send mode; async by default.
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	/**
	 * Specify a timeout in milliseconds for how long this
	 * {@link KafkaProducerMessageHandler} should wait for send operation results.
	 * Defaults to the kafka {@code delivery.timeout.ms} property + 5 seconds. The timeout
	 * is applied Also applies when sending to the success or failure channels.
	 * @param sendTimeout the timeout to wait for result for a send operation.
	 */
	@Override
	public final void setSendTimeout(long sendTimeout) {
		super.setSendTimeout(sendTimeout);
		setSendTimeoutExpression(new ValueExpression<>(sendTimeout));
	}

	/**
	 * Specify a SpEL expression to evaluate a timeout in milliseconds for how long this
	 * {@link KafkaProducerMessageHandler} should wait for send operation results.
	 * Defaults to the kafka {@code delivery.timeout.ms} property + 5 seconds. The timeout
	 * is applied only in {@link #sync} mode. If this expression yields a result that is
	 * less than that value, the higher value is used.
	 * @param sendTimeoutExpression the {@link Expression} for timeout to wait for result
	 * for a send operation.
	 * @see #setTimeoutBuffer(int)
	 */
	public void setSendTimeoutExpression(Expression sendTimeoutExpression) {
		Assert.notNull(sendTimeoutExpression, "'sendTimeoutExpression' must not be null");
		this.sendTimeoutExpression = sendTimeoutExpression;
	}

	/**
	 * Set the failure channel. After a send failure, an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be sent
	 * to this channel with a payload of a {@link KafkaSendFailureException} with the
	 * failed message and cause.
	 * @param sendFailureChannel the failure channel.
	 */
	public void setSendFailureChannel(MessageChannel sendFailureChannel) {
		this.sendFailureChannel = sendFailureChannel;
	}

	/**
	 * Set the failure channel name. After a send failure, an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be
	 * sent to this channel name with a payload of a {@link KafkaSendFailureException}
	 * with the failed message and cause.
	 * @param sendFailureChannelName the failure channel name.
	 */
	public void setSendFailureChannelName(String sendFailureChannelName) {
		this.sendFailureChannelName = sendFailureChannelName;
	}

	/**
	 * Set the success channel.
	 * @param sendSuccessChannel the Success channel.
	 */
	public void setSendSuccessChannel(MessageChannel sendSuccessChannel) {
		this.sendSuccessChannel = sendSuccessChannel;
	}

	/**
	 * Set the Success channel name.
	 * @param sendSuccessChannelName the Success channel name.
	 */
	public void setSendSuccessChannelName(String sendSuccessChannelName) {
		this.sendSuccessChannelName = sendSuccessChannelName;
	}

	/**
	 * Set the futures channel.
	 * @param futuresChannel the futures channel.
	 */
	public void setFuturesChannel(MessageChannel futuresChannel) {
		this.futuresChannel = futuresChannel;
	}

	/**
	 * Set the futures channel name.
	 * @param futuresChannelName the futures channel name.
	 */
	public void setFuturesChannelName(String futuresChannelName) {
		this.futuresChannelName = futuresChannelName;
	}

	/**
	 * Set the error message strategy implementation to use when sending error messages after
	 * send failures. Cannot be null.
	 * @param errorMessageStrategy the implementation.
	 */
	public void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		Assert.notNull(errorMessageStrategy, "'errorMessageStrategy' cannot be null");
		this.errorMessageStrategy = errorMessageStrategy;
	}

	/**
	 * Set a message converter for gateway replies.
	 * @param messageConverter the converter.
	 * @see #setReplyPayloadType(Type)
	 */
	public void setReplyMessageConverter(RecordMessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' cannot be null");
		this.replyMessageConverter = messageConverter;
	}

	/**
	 * When using a type-aware message converter (such as {@code StringJsonMessageConverter},
	 * set the payload type the converter should create. Defaults to {@link Object}.
	 * @param payloadType the type.
	 * @see #setReplyMessageConverter(RecordMessageConverter)
	 */
	public void setReplyPayloadType(Type payloadType) {
		Assert.notNull(payloadType, "'payloadType' cannot be null");
		this.replyPayloadType = payloadType;
	}

	/**
	 * Set a {@link ProducerRecordCreator} to create the {@link ProducerRecord}. Ignored
	 * if {@link #setUseTemplateConverter(boolean) useTemplateConverter} is true.
	 * @param producerRecordCreator the creator.
	 * @see #setUseTemplateConverter(boolean)
	 */
	public void setProducerRecordCreator(ProducerRecordCreator<K, V> producerRecordCreator) {
		Assert.notNull(producerRecordCreator, "'producerRecordCreator' cannot be null");
		this.producerRecordCreator = producerRecordCreator;
	}

	/**
	 * Set a buffer, in milliseconds, added to the configured {@code delivery.timeout.ms}
	 * to determine the minimum time to wait for the send future completion when
	 * {@link #setSync(boolean) sync} is true.
	 * @param timeoutBuffer the buffer.
	 * @see #setSendTimeoutExpression(Expression)
	 */
	public void setTimeoutBuffer(int timeoutBuffer) {
		this.timeoutBuffer = timeoutBuffer;
	}

	/**
	 * Set to true to use the template's message converter to create the
	 * {@link ProducerRecord} instead of the
	 * {@link #setProducerRecordCreator(ProducerRecordCreator) producerRecordCreator}.
	 * @param useTemplateConverter true to use the converter.
	 * @since 5.5.5
	 * @see #setProducerRecordCreator(ProducerRecordCreator)
	 */
	public void setUseTemplateConverter(boolean useTemplateConverter) {
		this.useTemplateConverter = useTemplateConverter;
	}

	/**
	 * Set the time to wait for partition assignment, when used as a gateway.
	 * @param assignmentDuration the assignmentDuration to set.
	 * @since 6.0
	 */
	public void setAssignmentDuration(Duration assignmentDuration) {
		Assert.notNull(assignmentDuration, "'assignmentDuration' cannot be null");
		this.assignmentDuration = assignmentDuration;
	}

	@Override
	public String getComponentType() {
		return this.isGateway ? "kafka:outbound-gateway" : "kafka:outbound-channel-adapter";
	}

	@Nullable
	protected MessageChannel getSendFailureChannel() {
		if (this.sendFailureChannel != null) {
			return this.sendFailureChannel;
		}
		else if (this.sendFailureChannelName != null) {
			this.sendFailureChannel = getChannelResolver().resolveDestination(this.sendFailureChannelName);
			return this.sendFailureChannel;
		}
		return null;
	}

	protected MessageChannel getSendSuccessChannel() {
		if (this.sendSuccessChannel != null) {
			return this.sendSuccessChannel;
		}
		else if (this.sendSuccessChannelName != null) {
			this.sendSuccessChannel = getChannelResolver().resolveDestination(this.sendSuccessChannelName);
			return this.sendSuccessChannel;
		}
		return null;
	}

	protected MessageChannel getFuturesChannel() {
		if (this.futuresChannel != null) {
			return this.futuresChannel;
		}
		else if (this.futuresChannelName != null) {
			this.futuresChannel = getChannelResolver().resolveDestination(this.futuresChannelName);
			return this.futuresChannel;
		}
		return null;
	}

	@Override
	protected void doInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	public void start() {
		this.running.set(true);
	}

	@Override
	public void stop() {
		if (this.running.compareAndSet(true, false) && (!this.transactional || this.allowNonTransactional)) {
			this.kafkaTemplate.flush();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@SuppressWarnings("unchecked") // NOSONAR - complexity
	@Override
	protected Object handleRequestMessage(final Message<?> message) {
		final ProducerRecord<K, V> producerRecord;
		boolean flush =
				Boolean.TRUE.equals(this.flushExpression.getValue(this.evaluationContext, message, Boolean.class));
		if (message.getPayload() instanceof ProducerRecord) {
			producerRecord = (ProducerRecord<K, V>) message.getPayload();
		}
		else {
			producerRecord = createProducerRecord(message);
			if (flush) {
				producerRecord.headers().remove(KafkaIntegrationHeaders.FLUSH);
			}
		}
		Object futureToken = message.getHeaders().get(KafkaIntegrationHeaders.FUTURE_TOKEN);
		if (futureToken != null) {
			producerRecord.headers().remove(KafkaIntegrationHeaders.FUTURE_TOKEN);
		}
		CompletableFuture<SendResult<K, V>> sendFuture;
		RequestReplyFuture<K, V, Object> gatewayFuture = null;
		try {
			if (this.isGateway) {
				waitForAssignment();
				addReplyTopicIfAny(message.getHeaders(), producerRecord.headers());
				gatewayFuture =
						((ReplyingKafkaTemplate<K, V, Object>) this.kafkaTemplate).sendAndReceive(producerRecord);
				sendFuture = gatewayFuture.getSendFuture();
			}
			else {
				if (this.transactional && !this.kafkaTemplate.inTransaction() && !this.allowNonTransactional) {
					sendFuture = this.kafkaTemplate.executeInTransaction(template -> template.send(producerRecord));
				}
				else {
					sendFuture = this.kafkaTemplate.send(producerRecord);
				}
			}
		}
		catch (RuntimeException rtex) {
			sendFailure(message, producerRecord, getSendFailureChannel(), rtex);
			throw rtex;
		}
		sendFutureIfRequested(sendFuture, futureToken);
		if (flush) {
			this.kafkaTemplate.flush();
		}
		try {
			processSendResult(message, producerRecord, sendFuture, getSendSuccessChannel());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessageHandlingException(message, e);
		}
		catch (ExecutionException e) {
			throw new MessageHandlingException(message, e.getCause()); // NOSONAR
		}
		return processReplyFuture(gatewayFuture);
	}

	@SuppressWarnings("unchecked")
	private ProducerRecord<K, V> createProducerRecord(final Message<?> message) {
		MessageHeaders messageHeaders = message.getHeaders();
		String topic = this.topicExpression != null ?
				this.topicExpression.getValue(this.evaluationContext, message, String.class)
				: messageHeaders.get(KafkaHeaders.TOPIC, String.class);
		if (topic == null) {
			topic = this.kafkaTemplate.getDefaultTopic();
		}
		if (this.useTemplateConverter) {
			return (ProducerRecord<K, V>) this.kafkaTemplate.getMessageConverter().fromMessage(message, topic);
		}

		Assert.state(StringUtils.hasText(topic), "The 'topic' can not be empty or null");

		Integer partitionId = this.partitionIdExpression != null ?
				this.partitionIdExpression.getValue(this.evaluationContext, message, Integer.class)
				: messageHeaders.get(KafkaHeaders.PARTITION, Integer.class);

		Object messageKey = this.messageKeyExpression != null
				? this.messageKeyExpression.getValue(this.evaluationContext, message)
				: messageHeaders.get(KafkaHeaders.KEY);

		Long timestamp = this.timestampExpression != null
				? this.timestampExpression.getValue(this.evaluationContext, message, Long.class)
				: messageHeaders.get(KafkaHeaders.TIMESTAMP, Long.class);

		V payload = (V) message.getPayload();
		if (payload instanceof KafkaNull) {
			payload = null;
		}

		Headers headers = new RecordHeaders();
		if (this.headerMapper != null) {
			this.headerMapper.fromHeaders(messageHeaders, headers);
		}

		return this.producerRecordCreator.create(message, topic, partitionId, timestamp, (K) messageKey, payload,
				headers);
	}

	private void waitForAssignment() {
		ReplyingKafkaTemplate<?, ?, ?> rkt = (ReplyingKafkaTemplate<?, ?, ?>) this.kafkaTemplate;
		try {
			rkt.waitForAssignment(this.assignmentDuration);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Nullable
	private void addReplyTopicIfAny(MessageHeaders messageHeaders, Headers headers) {
		if (this.isGateway) {
			Object replyHeader = messageHeaders.get(KafkaHeaders.REPLY_TOPIC);
			if (replyHeader instanceof String topicString) {
				headers.add(KafkaHeaders.REPLY_TOPIC, topicString.getBytes(StandardCharsets.UTF_8));
			}
			else if (replyHeader instanceof byte[] topicBytes) {
				headers.add(KafkaHeaders.REPLY_TOPIC, topicBytes);
			}
			else if (replyHeader != null) {
				throw new IllegalStateException(KafkaHeaders.REPLY_TOPIC + " must be String or byte[]");
			}
		}
	}

	private void sendFutureIfRequested(CompletableFuture<SendResult<K, V>> sendFuture, Object futureToken) {

		if (futureToken != null) {
			MessageChannel futures = getFuturesChannel();
			if (futures != null) {
				try {
					futures.send(getMessageBuilderFactory()
							.withPayload(sendFuture)
							.setHeader(KafkaIntegrationHeaders.FUTURE_TOKEN, futureToken)
							.build());
				}
				catch (Exception e) {
					this.logger.error(e, "Failed to send sendFuture");
				}
			}
		}
	}

	public void processSendResult(final Message<?> message, final ProducerRecord<K, V> producerRecord,
			CompletableFuture<SendResult<K, V>> future, MessageChannel metadataChannel)
			throws InterruptedException, ExecutionException {

		final MessageChannel failureChannel = getSendFailureChannel();
		if (failureChannel != null || metadataChannel != null) {
			future.whenComplete((sendResult, exception) -> {
				if (exception == null) {
					if (metadataChannel != null) {
						KafkaProducerMessageHandler.this.messagingTemplate.send(metadataChannel,
								getMessageBuilderFactory()
										.fromMessage(message)
										.setHeader(KafkaHeaders.RECORD_METADATA, sendResult.getRecordMetadata())
										.build());
					}
				}
				else {
					sendFailure(message, producerRecord, failureChannel, exception);
				}
			});
		}

		if (this.sync || this.isGateway) {
			Long sendTimeout = this.sendTimeoutExpression.getValue(this.evaluationContext, message, Long.class);
			if (sendTimeout != null && sendTimeout <= this.deliveryTimeoutMsProperty) {
				this.logger.debug(() -> "'sendTimeout' increased to "
						+ (this.deliveryTimeoutMsProperty + this.timeoutBuffer)
						+ "ms; it must be greater than the 'delivery.timeout.ms' Kafka producer "
						+ "property to avoid false failures");
				sendTimeout = this.deliveryTimeoutMsProperty + this.timeoutBuffer;
			}
			if (sendTimeout == null || sendTimeout < 0) {
				future.get();
			}
			else {
				try {
					future.get(sendTimeout, TimeUnit.MILLISECONDS);
				}
				catch (TimeoutException te) {
					throw new MessageTimeoutException(message, "Timeout waiting for response from KafkaProducer", te);
				}
			}
		}
	}

	private void sendFailure(final Message<?> message, final ProducerRecord<K, V> producerRecord,
			@Nullable MessageChannel failureChannel, Throwable exception) {

		if (failureChannel != null) {
			KafkaProducerMessageHandler.this.messagingTemplate.send(failureChannel,
					KafkaProducerMessageHandler.this.errorMessageStrategy.buildErrorMessage(
							new KafkaSendFailureException(message, producerRecord, exception), null));
		}
	}

	private Future<?> processReplyFuture(@Nullable RequestReplyFuture<?, ?, Object> future) {
		if (future == null) {
			return null;
		}
		return new ConvertingReplyFuture(future);
	}

	private final class ConvertingReplyFuture extends CompletableFuture<Object> {

		ConvertingReplyFuture(RequestReplyFuture<?, ?, Object> future) {
			addCallback(future);
		}

		private void addCallback(final RequestReplyFuture<?, ?, Object> future) {
			future.whenComplete((result, exception) -> {
				if (exception == null) {
					try {
						complete(dontLeakHeaders(
								KafkaProducerMessageHandler.this.replyMessageConverter.toMessage(result, null, null,
										KafkaProducerMessageHandler.this.replyPayloadType)));
					}
					catch (Exception ex) {
						completeExceptionally(ex);
					}
				}
				else {
					completeExceptionally(exception);
				}
			});
		}

		private Message<?> dontLeakHeaders(Message<?> message) {
			if (message.getHeaders() instanceof KafkaMessageHeaders) {
				Map<String, Object> headers = ((KafkaMessageHeaders) message.getHeaders()).getRawHeaders();
				headers.remove(KafkaHeaders.CORRELATION_ID);
				headers.remove(KafkaHeaders.REPLY_TOPIC);
				headers.remove(KafkaHeaders.REPLY_PARTITION);
				return message;
			}
			else {
				return getMessageBuilderFactory().fromMessage(message)
						.removeHeader(KafkaHeaders.CORRELATION_ID)
						.removeHeader(KafkaHeaders.REPLY_TOPIC)
						.removeHeader(KafkaHeaders.REPLY_PARTITION)
						.build();
			}
		}

	}

	/**
	 * Creates a {@link ProducerRecord} from a {@link Message} and/or properties
	 * derived from configuration and/or the message.
	 *
	 * @param <K> the key type.
	 * @param <V> the value type.
	 *
	 * @since 5.4
	 *
	 */
	@FunctionalInterface
	public interface ProducerRecordCreator<K, V> {

		/**
		 * Create a record.
		 * @param message the outbound message.
		 * @param topic the topic.
		 * @param partition the partition.
		 * @param timestamp the timestamp.
		 * @param key the key.
		 * @param value the value.
		 * @param headers the headers.
		 * @return the record.
		 */
		ProducerRecord<K, V> create(Message<?> message, String topic, Integer partition, Long timestamp, K key, V value,
				Headers headers);

	}

}
