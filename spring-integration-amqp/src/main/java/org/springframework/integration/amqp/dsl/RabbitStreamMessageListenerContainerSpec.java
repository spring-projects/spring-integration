/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import java.util.function.Consumer;

import com.rabbitmq.stream.Codec;
import com.rabbitmq.stream.Environment;
import org.aopalliance.aop.Advice;

import org.springframework.lang.Nullable;
import org.springframework.rabbit.stream.listener.ConsumerCustomizer;
import org.springframework.rabbit.stream.listener.StreamListenerContainer;
import org.springframework.rabbit.stream.support.converter.StreamMessageConverter;

/**
 * Spec for {@link StreamListenerContainer}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 6.0
 *
 */
public class RabbitStreamMessageListenerContainerSpec extends
		MessageListenerContainerSpec<RabbitStreamMessageListenerContainerSpec, StreamListenerContainer> {

	RabbitStreamMessageListenerContainerSpec(StreamListenerContainer container) {
		this.target = container;
	}

	RabbitStreamMessageListenerContainerSpec(Environment environment, @Nullable Codec codec) {
		this.target = new StreamListenerContainer(environment, codec);
	}

	/**
	 * Enable Single Active Consumer on a Super Stream.
	 * Mutually exclusive with {@link #queueName(String...)}.
	 * @param superStream the stream.
	 * @param name the consumer name.
	 * @return this spec.
	 */
	public RabbitStreamMessageListenerContainerSpec superStream(String superStream, String name) {
		return superStream(superStream, name, 1);
	}

	/**
	 * Enable Single Active Consumer on a Super Stream.
	 * Mutually exclusive with {@link #queueName(String...)}.
	 * @param superStream the stream.
	 * @param name the consumer name.
	 * @param consumers the number of consumers.
	 * @return this spec.
	 * @since 6.1
	 */
	public RabbitStreamMessageListenerContainerSpec superStream(String superStream, String name, int consumers) {
		this.target.superStream(superStream, name, consumers);
		return this;
	}

	/**
	 * Set a stream message converter.
	 * @param converter the converter.
	 * @return this spec.
	 */
	public RabbitStreamMessageListenerContainerSpec streamConverter(StreamMessageConverter converter) {
		this.target.setStreamConverter(converter);
		return this;
	}

	/**
	 * Set a consumer customizer.
	 * @param customizer the customizer.
	 * @return this spec.
	 */
	public RabbitStreamMessageListenerContainerSpec consumerCustomizer(ConsumerCustomizer customizer) {
		this.target.setConsumerCustomizer(customizer);
		return this;
	}

	/**
	 * @param adviceChain the adviceChain.
	 * @return the spec.
	 * @see StreamListenerContainer#setAdviceChain(Advice[])
	 */
	public RabbitStreamMessageListenerContainerSpec adviceChain(Advice... adviceChain) {
		this.target.setAdviceChain(adviceChain);
		return this;
	}

	/**
	 * Perform additional configuration of the container.
	 * @param consumer a consumer for the container.
	 * @return this spec.
	 */
	public RabbitStreamMessageListenerContainerSpec configure(Consumer<StreamListenerContainer> consumer) {
		consumer.accept(this.target);
		return this;
	}

}
