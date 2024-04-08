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

package org.springframework.integration.dsl;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.management.observation.MessageReceiverObservationConvention;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;

/**
 * An {@link IntegrationComponentSpec} for
 * {@link org.springframework.integration.core.MessageProducer}s.
 *
 * @param <S> the target {@link MessageProducerSpec} implementation type.
 * @param <P> the target {@link MessageProducerSupport} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class MessageProducerSpec<S extends MessageProducerSpec<S, P>, P extends MessageProducerSupport>
		extends IntegrationComponentSpec<S, P> {

	public MessageProducerSpec(@Nullable P producer) {
		this.target = producer;
	}

	/**
	 * {@inheritDoc}
	 * Configure the message producer's bean name.
	 */
	@Override
	public S id(@Nullable String id) {
		this.target.setBeanName(id);
		return super.id(id);
	}

	/**
	 * @param phase the phase.
	 * @return the spec.
	 * @see org.springframework.context.SmartLifecycle
	 */
	public S phase(int phase) {
		this.target.setPhase(phase);
		return _this();
	}

	/**
	 * @param autoStartup the autoStartup.
	 * @return the spec.
	 * @see org.springframework.context.SmartLifecycle
	 */
	public S autoStartup(boolean autoStartup) {
		this.target.setAutoStartup(autoStartup);
		return _this();
	}

	/**
	 * Specify the role for the endpoint.
	 * Such endpoints can be started/stopped as a group.
	 * @param role the role for this endpoint.
	 * @return the endpoint spec
	 * @since 6.1.8
	 * @see org.springframework.context.SmartLifecycle
	 * @see org.springframework.integration.support.SmartLifecycleRoleController
	 */
	public S role(String role) {
		this.target.setRole(role);
		return _this();
	}

	/**
	 * Specify the {@code outputChannel} for the
	 * {@link org.springframework.integration.core.MessageProducer}.
	 * @param outputChannel the outputChannel.
	 * @return the spec.
	 * @see MessageProducerSupport#setOutputChannel(MessageChannel)
	 */
	public S outputChannel(MessageChannel outputChannel) {
		this.target.setOutputChannel(outputChannel);
		return _this();
	}

	/**
	 * Specify the bean name of the {@code outputChannel} for the
	 * {@link org.springframework.integration.core.MessageProducer}.
	 * @param outputChannel the outputChannel bean name.
	 * @return the spec.
	 * @see MessageProducerSupport#setOutputChannelName(String)
	 */
	public S outputChannel(String outputChannel) {
		this.target.setOutputChannelName(outputChannel);
		return _this();
	}

	/**
	 * Configure the {@link MessageChannel} to which error messages will be sent.
	 * @param errorChannel the errorChannel.
	 * @return the spec.
	 * @see MessageProducerSupport#setErrorChannel(MessageChannel)
	 */
	public S errorChannel(MessageChannel errorChannel) {
		this.target.setErrorChannel(errorChannel);
		return _this();
	}

	/**
	 * Configure the bean name of the {@link MessageChannel} to which error messages will be sent.
	 * @param errorChannel the errorChannel bean name.
	 * @return the spec.
	 * @see MessageProducerSupport#setErrorChannelName(String)
	 */
	public S errorChannel(String errorChannel) {
		this.target.setErrorChannelName(errorChannel);
		return _this();
	}

	/**
	 * Configure the default timeout value to use for send operations.
	 * May be overridden for individual messages.
	 * @param sendTimeout the send timeout in milliseconds
	 * @return the spec.
	 * @since 5.0.2
	 * @see MessageProducerSupport#setSendTimeout
	 */
	public S sendTimeout(long sendTimeout) {
		this.target.setSendTimeout(sendTimeout);
		return _this();
	}

	/**
	 * Whether component should be tracked or not by message history.
	 * @param shouldTrack the tracking flag
	 * @return the spec.
	 * @since 5.0.2
	 * @see MessageProducerSupport#setShouldTrack(boolean)
	 */
	public S shouldTrack(boolean shouldTrack) {
		this.target.setShouldTrack(shouldTrack);
		return _this();
	}

	/**
	 * Set an {@link ErrorMessageStrategy} to use to build an error message when a exception occurs.
	 * @param errorMessageStrategy the {@link ErrorMessageStrategy}.
	 * @return the spec.
	 * @since 5.0.2
	 * @see MessageProducerSupport#setErrorMessageStrategy(ErrorMessageStrategy)
	 */
	public S errorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		this.target.setErrorMessageStrategy(errorMessageStrategy);
		return _this();
	}

	/**
	 * Provide a custom {@link MessageReceiverObservationConvention}.
	 * @param observationConvention the observation convention to use.
	 * @return the spec.
	 * @since 6.0.8
	 * @see MessageProducerSupport#setObservationConvention(MessageReceiverObservationConvention)
	 */
	public S observationConvention(MessageReceiverObservationConvention observationConvention) {
		this.target.setObservationConvention(observationConvention);
		return _this();
	}

}
