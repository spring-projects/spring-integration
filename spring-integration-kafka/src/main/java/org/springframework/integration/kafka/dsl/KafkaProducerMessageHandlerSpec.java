/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.kafka.dsl;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.KafkaHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} implementation for the {@link KafkaProducerMessageHandler}.
 *
 * @param <K> the key type.
 * @param <V> the value type.
 * @param <S> the {@link KafkaProducerMessageHandlerSpec} extension type.
 *
 * @author Artem Bilan
 * @author Biju Kunjummen
 * @author Gary Russell
 *
 * @since 5.4
 */
public class KafkaProducerMessageHandlerSpec<K, V, S extends KafkaProducerMessageHandlerSpec<K, V, S>>
		extends MessageHandlerSpec<S, KafkaProducerMessageHandler<K, V>> {

	KafkaProducerMessageHandlerSpec(KafkaTemplate<K, V> kafkaTemplate) {
		this.target = new KafkaProducerMessageHandler<>(kafkaTemplate);
	}

	/**
	 * Configure the Kafka topic to send messages.
	 * @param topic the Kafka topic name.
	 * @return the spec.
	 */
	public S topic(String topic) {
		return topicExpression(new LiteralExpression(topic));
	}

	/**
	 * Configure a SpEL expression to determine the Kafka topic at runtime against
	 * request Message as a root object of evaluation context.
	 * @param topicExpression the topic SpEL expression.
	 * @return the spec.
	 */
	public S topicExpression(String topicExpression) {
		return topicExpression(PARSER.parseExpression(topicExpression));
	}

	/**
	 * Configure an {@link Expression} to determine the Kafka topic at runtime against
	 * request Message as a root object of evaluation context.
	 * @param topicExpression the topic expression.
	 * @return the spec.
	 */
	public S topicExpression(Expression topicExpression) {
		this.target.setTopicExpression(topicExpression);
		return _this();
	}

	/**
	 * Configure a {@link Function} that will be invoked at runtime to determine the topic
	 * to which a message will be sent. Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .<Foo>topic(m -> m.getPayload().getTopic())
	 * }
	 * </pre>
	 * @param topicFunction the topic function.
	 * @param <P> the expected payload type.
	 * @return the current {@link KafkaProducerMessageHandlerSpec}.
	 * @see FunctionExpression
	 */
	public <P> S topic(Function<Message<P>, String> topicFunction) {
		return topicExpression(new FunctionExpression<>(topicFunction));
	}

	/**
	 * Configure a SpEL expression to determine the Kafka message key to store at runtime against
	 * request Message as a root object of evaluation context.
	 * @param messageKeyExpression the message key SpEL expression.
	 * @return the spec.
	 */
	public S messageKeyExpression(String messageKeyExpression) {
		return messageKeyExpression(PARSER.parseExpression(messageKeyExpression));
	}

	/**
	 * Configure the message key to store message in Kafka topic.
	 * @param messageKey the message key to use.
	 * @return the spec.
	 */
	public S messageKey(String messageKey) {
		return messageKeyExpression(new LiteralExpression(messageKey));
	}

	/**
	 * Configure an {@link Expression} to determine the Kafka message key to store at runtime against
	 * request Message as a root object of evaluation context.
	 * @param messageKeyExpression the message key expression.
	 * @return the spec.
	 */
	public S messageKeyExpression(Expression messageKeyExpression) {
		this.target.setMessageKeyExpression(messageKeyExpression);
		return _this();
	}

	/**
	 * Configure a {@link Function} that will be invoked at runtime to determine the
	 * message key under which a message will be stored in the topic. Typically used with
	 * a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .<Foo>messageKey(m -> m.getPayload().getKey())
	 * }
	 * </pre>
	 * @param messageKeyFunction the message key function.
	 * @param <P> the expected payload type.
	 * @return the current {@link KafkaProducerMessageHandlerSpec}.
	 * @see FunctionExpression
	 */
	public <P> S messageKey(Function<Message<P>, ?> messageKeyFunction) {
		return messageKeyExpression(new FunctionExpression<>(messageKeyFunction));
	}

	/**
	 * Configure a partitionId of Kafka topic.
	 * @param partitionId the partitionId to use.
	 * @return the spec.
	 */
	public S partitionId(Integer partitionId) {
		return partitionIdExpression(new ValueExpression<Integer>(partitionId));
	}

	/**
	 * Configure a SpEL expression to determine the topic partitionId at runtime against
	 * request Message as a root object of evaluation context.
	 * @param partitionIdExpression the partitionId expression to use.
	 * @return the spec.
	 */
	public S partitionIdExpression(String partitionIdExpression) {
		return partitionIdExpression(PARSER.parseExpression(partitionIdExpression));
	}

	/**
	 * Configure a {@link Function} that will be invoked at runtime to determine the
	 * partition id under which a message will be stored in the topic. Typically used with
	 * a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .partitionId(m -> m.getHeaders().get("partitionId", Integer.class))
	 * }
	 * </pre>
	 * @param partitionIdFunction the partitionId function.
	 * @param <P> the expected payload type.
	 * @return the spec.
	 */
	public <P> S partitionId(Function<Message<P>, Integer> partitionIdFunction) {
		return partitionIdExpression(new FunctionExpression<>(partitionIdFunction));
	}

	/**
	 * Configure an {@link Expression} to determine the topic partitionId at runtime against
	 * request Message as a root object of evaluation context.
	 * @param partitionIdExpression the partitionId expression to use.
	 * @return the spec.
	 */
	public S partitionIdExpression(Expression partitionIdExpression) {
		this.target.setPartitionIdExpression(partitionIdExpression);
		return _this();
	}

	/**
	 * Configure a SpEL expression to determine the timestamp at runtime against a
	 * request Message as a root object of evaluation context.
	 * @param timestampExpression the timestamp expression to use.
	 * @return the spec.
	 */
	public S timestampExpression(String timestampExpression) {
		return this.timestampExpression(PARSER.parseExpression(timestampExpression));
	}

	/**
	 * Configure a {@link Function} that will be invoked at runtime to determine the Kafka
	 * record timestamp will be stored in the topic. Typically used with a Java 8 Lambda
	 * expression:
	 * <pre class="code">
	 * {@code
	 * .timestamp(m -> m.getHeaders().get("mytimestamp_header", Long.class))
	 * }
	 * </pre>
	 * @param timestampFunction the timestamp function.
	 * @param <P> the expected payload type.
	 * @return the spec.
	 */
	public <P> S timestamp(Function<Message<P>, Long> timestampFunction) {
		return timestampExpression(new FunctionExpression<>(timestampFunction));
	}

	/**
	 * Configure an {@link Expression} to determine the timestamp at runtime against a
	 * request Message as a root object of evaluation context.
	 * @param timestampExpression the timestamp expression to use.
	 * @return the spec.
	 */
	public S timestampExpression(Expression timestampExpression) {
		this.target.setTimestampExpression(timestampExpression);
		return _this();
	}

	/**
	 * Configure a SpEL expression to determine whether or not to flush the producer after
	 * a send. By default the producer is flushed if a header {@code kafka_flush} has a
	 * value {@link Boolean#TRUE}.
	 * @param flushExpression the timestamp expression to use.
	 * @return the spec.
	 */
	public S flushExpression(String flushExpression) {
		return this.flushExpression(PARSER.parseExpression(flushExpression));
	}

	/**
	 * Configure a {@link Function} that will be invoked at runtime to determine whether
	 * or not to flush the producer after a send. By default the producer is flushed if a
	 * header {@code kafka_flush} has a value {@link Boolean#TRUE}. Typically used with a
	 * Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .flush(m -> m.getPayload().shouldFlush())
	 * }
	 * </pre>
	 * @param flushFunction the flush function.
	 * @param <P> the expected payload type.
	 * @return the spec.
	 */
	public <P> S flush(Function<Message<P>, Boolean> flushFunction) {
		return flushExpression(new FunctionExpression<>(flushFunction));
	}

	/**
	 * Configure an {@link Expression} to determine whether or not to flush the producer
	 * after a send. By default the producer is flushed if a header {@code kafka_flush}
	 * has a value {@link Boolean#TRUE}.
	 * @param flushExpression the timestamp expression to use.
	 * @return the spec.
	 */
	public S flushExpression(Expression flushExpression) {
		this.target.setFlushExpression(flushExpression);
		return _this();
	}

	/**
	 * A {@code boolean} indicating if the {@link KafkaProducerMessageHandler}
	 * should wait for the send operation results or not. Defaults to {@code false}.
	 * In {@code sync} mode a downstream send operation exception will be re-thrown.
	 * @param sync the send mode; async by default.
	 * @return the spec.
	 */
	public S sync(boolean sync) {
		this.target.setSync(sync);
		return _this();
	}

	/**
	 * Specify a timeout in milliseconds how long {@link KafkaProducerMessageHandler}
	 * should wait wait for send operation results. Defaults to 10 seconds.
	 * @param sendTimeout the timeout to wait for result fo send operation.
	 * @return the spec.
	 */
	public S sendTimeout(long sendTimeout) {
		this.target.setSendTimeout(sendTimeout);
		return _this();
	}

	/**
	 * Specify a header mapper to map spring messaging headers to Kafka headers.
	 * @param mapper the mapper.
	 * @return the spec.
	 */
	public S headerMapper(KafkaHeaderMapper mapper) {
		this.target.setHeaderMapper(mapper);
		return _this();
	}

	/**
	 * Set the channel to which successful send results are sent.
	 * @param sendSuccessChannel the channel.
	 * @return the spec.
	 * @since 3.0.2
	 */
	public S sendSuccessChannel(MessageChannel sendSuccessChannel) {
		this.target.setSendSuccessChannel(sendSuccessChannel);
		return _this();
	}

	/**
	 * Set the channel to which successful send results are sent.
	 * @param sendSuccessChannel the channel name.
	 * @return the spec.
	 * @since 3.0.2
	 */
	public S sendSuccessChannel(String sendSuccessChannel) {
		this.target.setSendSuccessChannelName(sendSuccessChannel);
		return _this();
	}

	/**
	 * Set the channel to which failed send results are sent.
	 * @param sendFailureChannel the channel.
	 * @return the spec.
	 * @since 3.0.2
	 */
	public S sendFailureChannel(MessageChannel sendFailureChannel) {
		this.target.setSendFailureChannel(sendFailureChannel);
		return _this();
	}

	/**
	 * Set the channel to which failed send results are sent.
	 * @param sendFailureChannel the channel name.
	 * @return the spec.
	 * @since 3.0.2
	 */
	public S sendFailureChannel(String sendFailureChannel) {
		this.target.setSendFailureChannelName(sendFailureChannel);
		return _this();
	}

	/**
	 * Set the channel to which send futures are sent.
	 * @param futuresChannel the channel.
	 * @return the spec.
	 * @since 5.4
	 */
	public S futuresChannel(MessageChannel futuresChannel) {
		this.target.setFuturesChannel(futuresChannel);
		return _this();
	}

	/**
	 * Set the channel to which send futures are sent.
	 * @param futuresChannel the channel name.
	 * @return the spec.
	 * @since 5.4
	 */
	public S futuresChannel(String futuresChannel) {
		this.target.setFuturesChannelName(futuresChannel);
		return _this();
	}

	/**
	 * A {@link KafkaTemplate}-based {@link KafkaProducerMessageHandlerSpec} extension.
	 *
	 * @param <K> the key type.
	 * @param <V> the value type.
	 */
	public static class KafkaProducerMessageHandlerTemplateSpec<K, V> extends KafkaProducerMessageHandlerSpec<K, V, KafkaProducerMessageHandlerTemplateSpec<K, V>>
			implements ComponentsRegistration {

		private final KafkaTemplateSpec<K, V> kafkaTemplateSpec;

		@SuppressWarnings("unchecked")
		KafkaProducerMessageHandlerTemplateSpec(ProducerFactory<K, V> producerFactory) {
			super(new KafkaTemplate<>(producerFactory));
			this.kafkaTemplateSpec = new KafkaTemplateSpec<>((KafkaTemplate<K, V>) this.target.getKafkaTemplate());
		}

		/**
		 * Configure a Kafka Template by invoking the {@link Consumer} callback, with a
		 * {@link KafkaTemplateSpec} argument.
		 * @param configurer the configurer Java 8 Lambda.
		 * @return the spec.
		 */
		public KafkaProducerMessageHandlerTemplateSpec<K, V> configureKafkaTemplate(
				Consumer<KafkaTemplateSpec<K, V>> configurer) {
			Assert.notNull(configurer, "The 'configurer' cannot be null");
			configurer.accept(this.kafkaTemplateSpec);
			return _this();
		}

		@Override
		public Map<Object, String> getComponentsToRegister() {
			return Collections.singletonMap(this.kafkaTemplateSpec.get(), this.kafkaTemplateSpec.getId());
		}

	}

}

