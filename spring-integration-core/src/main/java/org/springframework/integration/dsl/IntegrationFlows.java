/*
 * Copyright 2016-2022 the original author or authors.
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

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * The central factory for fluent {@link IntegrationFlowBuilder} API.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Oleg Zhurakousky
 *
 * @since 5.0
 *
 * @see org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor
 *
 * @deprecated since version 6.0 in favor of fluent API methods straight from {@link IntegrationFlow} interface.
 */
@Deprecated(since = "6.0", forRemoval = true)
public final class IntegrationFlows {

	/**
	 * Populate the {@link MessageChannel} name to the new {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannelName the name of existing {@link MessageChannel} bean.
	 * The new {@link DirectChannel} bean will be created on context startup
	 * if there is no bean with this name.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(String messageChannelName) {
		return IntegrationFlow.from(messageChannelName);
	}

	/**
	 * Populate the {@link MessageChannel} name to the new {@link IntegrationFlowBuilder} chain.
	 * Typically for the {@link org.springframework.integration.channel.FixedSubscriberChannel} together
	 * with {@code fixedSubscriber = true}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannelName the name for {@link DirectChannel} or
	 * {@link org.springframework.integration.channel.FixedSubscriberChannel}
	 * to be created on context startup, not reference.
	 * The {@link MessageChannel} depends on the {@code fixedSubscriber} boolean argument.
	 * @param fixedSubscriber the boolean flag to determine if result {@link MessageChannel} should
	 * be {@link DirectChannel}, if {@code false} or
	 * {@link org.springframework.integration.channel.FixedSubscriberChannel}, if {@code true}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see DirectChannel
	 * @see org.springframework.integration.channel.FixedSubscriberChannel
	 */
	public static IntegrationFlowBuilder from(String messageChannelName, boolean fixedSubscriber) {
		return IntegrationFlow.from(messageChannelName, fixedSubscriber);
	}

	/**
	 * Populate the {@link MessageChannel} object to the
	 * {@link IntegrationFlowBuilder} chain using the fluent API from {@link MessageChannelSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannelSpec the MessageChannelSpec to populate {@link MessageChannel} instance.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see org.springframework.integration.dsl.MessageChannels
	 */
	public static IntegrationFlowBuilder from(MessageChannelSpec<?, ?> messageChannelSpec) {
		return IntegrationFlow.from(messageChannelSpec);
	}

	/**
	 * Populate the provided {@link MessageChannel} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code inputChannel}.
	 * @param messageChannel the {@link MessageChannel} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessageChannel messageChannel) {
		return IntegrationFlow.from(messageChannel);
	}

	/**
	 * Populate the {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the provided {@link MessageSourceSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param messageSourceSpec the {@link MessageSourceSpec} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSourceSpec and its implementations.
	 */
	public static IntegrationFlowBuilder from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec) {
		return IntegrationFlow.from(messageSourceSpec);
	}

	/**
	 * Populate the {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the provided {@link MessageSourceSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param messageSourceSpec the {@link MessageSourceSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide more options for the
	 * {@link org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSourceSpec
	 * @see SourcePollingChannelAdapterSpec
	 */
	public static IntegrationFlowBuilder from(MessageSourceSpec<?, ? extends MessageSource<?>> messageSourceSpec,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlow.from(messageSourceSpec, endpointConfigurer);
	}

	/**
	 * Provides {@link Supplier} as source of messages to the integration flow which will
	 * be triggered by the application context's default poller (which must be declared).
	 * @param messageSource the {@link Supplier} to populate.
	 * @param <T> the supplier type.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see Supplier
	 */
	public static <T> IntegrationFlowBuilder fromSupplier(Supplier<T> messageSource) {
		return IntegrationFlow.fromSupplier(messageSource);
	}

	/**
	 * Provides {@link Supplier} as source of messages to the integration flow.
	 * which will be triggered by a <b>provided</b>
	 * {@link org.springframework.integration.endpoint.SourcePollingChannelAdapter}.
	 * @param messageSource the {@link Supplier} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide more options for the
	 * {@link org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean}.
	 * @param <T> the supplier type.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see Supplier
	 */
	public static <T> IntegrationFlowBuilder fromSupplier(Supplier<T> messageSource,
			Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlow.fromSupplier(messageSource, endpointConfigurer);
	}

	/**
	 * Populate the provided {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * @param messageSource the {@link MessageSource} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSource
	 */
	public static IntegrationFlowBuilder from(MessageSource<?> messageSource) {
		return IntegrationFlow.from(messageSource);
	}

	/**
	 * Populate the provided {@link MessageSource} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageSource}.
	 * In addition use {@link SourcePollingChannelAdapterSpec} to provide options for the underlying
	 * {@link org.springframework.integration.endpoint.SourcePollingChannelAdapter} endpoint.
	 * @param messageSource the {@link MessageSource} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide more options for the
	 * {@link org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean}.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageSource
	 * @see SourcePollingChannelAdapterSpec
	 */
	public static IntegrationFlowBuilder from(MessageSource<?> messageSource,
			@Nullable Consumer<SourcePollingChannelAdapterSpec> endpointConfigurer) {
		return IntegrationFlow.from(messageSource, endpointConfigurer);
	}

	/**
	 * Populate the {@link MessageProducerSupport} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the {@link MessageProducerSpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageProducer}.
	 * @param messageProducerSpec the {@link MessageProducerSpec} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @see MessageProducerSpec
	 */
	public static IntegrationFlowBuilder from(MessageProducerSpec<?, ?> messageProducerSpec) {
		return IntegrationFlow.from(messageProducerSpec);
	}

	/**
	 * Populate the provided {@link MessageProducerSupport} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageProducer}.
	 * @param messageProducer the {@link MessageProducerSupport} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessageProducerSupport messageProducer) {
		return IntegrationFlow.from(messageProducer);
	}

	/**
	 * Populate the {@link MessagingGatewaySupport} object to the {@link IntegrationFlowBuilder} chain
	 * using the fluent API from the {@link MessagingGatewaySpec}.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessagingGateway}.
	 * @param inboundGatewaySpec the {@link MessagingGatewaySpec} to use.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessagingGatewaySpec<?, ?> inboundGatewaySpec) {
		return IntegrationFlow.from(inboundGatewaySpec);
	}

	/**
	 * Populate the provided {@link MessagingGatewaySupport} object to the {@link IntegrationFlowBuilder} chain.
	 * The {@link org.springframework.integration.dsl.IntegrationFlow} {@code startMessageProducer}.
	 * @param inboundGateway the {@link MessagingGatewaySupport} to populate.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(MessagingGatewaySupport inboundGateway) {
		return IntegrationFlow.from(inboundGateway);
	}

	/**
	 * Populate the {@link MessageChannel} to the new {@link IntegrationFlowBuilder}
	 * chain, which becomes as a {@code requestChannel} for the Messaging Gateway(s) built
	 * on the provided service interface.
	 * <p>A gateway proxy bean for provided service interface is registered under a name
	 * from the
	 * {@link org.springframework.integration.annotation.MessagingGateway#name()} if present
	 * or from the {@link IntegrationFlow} bean name plus {@code .gateway} suffix.
	 * @param serviceInterface the service interface class with an optional
	 * {@link org.springframework.integration.annotation.MessagingGateway} annotation.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	public static IntegrationFlowBuilder from(Class<?> serviceInterface) {
		return IntegrationFlow.from(serviceInterface);
	}

	/**
	 * Populate the {@link MessageChannel} to the new {@link IntegrationFlowBuilder}
	 * chain, which becomes as a {@code requestChannel} for the Messaging Gateway(s) built
	 * on the provided service interface.
	 * <p>A gateway proxy bean for provided service interface is based on the options
	 * configured via provided {@link Consumer}.
	 * @param serviceInterface the service interface class with an optional
	 * {@link org.springframework.integration.annotation.MessagingGateway} annotation.
	 * @param endpointConfigurer the {@link Consumer} to configure proxy bean for gateway.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @since 5.2
	 */
	public static IntegrationFlowBuilder from(Class<?> serviceInterface,
			@Nullable Consumer<GatewayProxySpec> endpointConfigurer) {
		return IntegrationFlow.from(serviceInterface, endpointConfigurer);
	}


	/**
	 * Populate a {@link FluxMessageChannel} to the {@link IntegrationFlowBuilder} chain
	 * and subscribe it to the provided {@link Publisher}.
	 * @param publisher the {@link Publisher} to subscribe to.
	 * @return new {@link IntegrationFlowBuilder}.
	 */
	@SuppressWarnings("overloads")
	public static IntegrationFlowBuilder from(Publisher<? extends Message<?>> publisher) {
		return IntegrationFlow.from(publisher);
	}

	/**
	 * Start the flow with a composition from the {@link IntegrationFlow}.
	 * @param other the {@link IntegrationFlow} from which to compose.
	 * @return new {@link IntegrationFlowBuilder}.
	 * @since 5.5.4
	 */
	@SuppressWarnings("overloads")
	public static IntegrationFlowBuilder from(IntegrationFlow other) {
		return IntegrationFlow.from(other);
	}

	private IntegrationFlows() {
	}

}
