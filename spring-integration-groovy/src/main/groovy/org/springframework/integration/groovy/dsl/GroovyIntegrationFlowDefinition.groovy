/*
 * Copyright 2022-2025 the original author or authors.
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

package org.springframework.integration.groovy.dsl

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.reactivestreams.Publisher
import org.springframework.integration.channel.BroadcastCapableChannel
import org.springframework.integration.channel.interceptor.WireTap
import org.springframework.integration.core.GenericHandler
import org.springframework.integration.core.GenericSelector
import org.springframework.integration.dsl.AggregatorSpec
import org.springframework.integration.dsl.BarrierSpec
import org.springframework.integration.dsl.BroadcastPublishSubscribeSpec
import org.springframework.integration.dsl.Channels
import org.springframework.integration.dsl.DelayerEndpointSpec
import org.springframework.integration.dsl.EnricherSpec
import org.springframework.integration.dsl.FilterEndpointSpec
import org.springframework.integration.dsl.GatewayEndpointSpec
import org.springframework.integration.dsl.GenericEndpointSpec
import org.springframework.integration.dsl.HeaderEnricherSpec
import org.springframework.integration.dsl.HeaderFilterSpec
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlowDefinition
import org.springframework.integration.dsl.MessageChannelSpec
import org.springframework.integration.dsl.MessageHandlerSpec
import org.springframework.integration.dsl.MessageProcessorSpec
import org.springframework.integration.dsl.PublishSubscribeSpec
import org.springframework.integration.dsl.RecipientListRouterSpec
import org.springframework.integration.dsl.ResequencerSpec
import org.springframework.integration.dsl.RouterSpec
import org.springframework.integration.dsl.ScatterGatherSpec
import org.springframework.integration.dsl.SplitterSpec
import org.springframework.integration.dsl.TransformerEndpointSpec
import org.springframework.integration.dsl.WireTapSpec
import org.springframework.integration.filter.MethodInvokingSelector
import org.springframework.integration.handler.BridgeHandler
import org.springframework.integration.handler.LoggingHandler
import org.springframework.integration.handler.MessageProcessor
import org.springframework.integration.handler.MessageTriggerAction
import org.springframework.integration.handler.ServiceActivatingHandler
import org.springframework.integration.router.AbstractMessageRouter
import org.springframework.integration.router.ErrorMessageExceptionTypeRouter
import org.springframework.integration.router.ExpressionEvaluatingRouter
import org.springframework.integration.router.MethodInvokingRouter
import org.springframework.integration.scattergather.ScatterGatherHandler
import org.springframework.integration.splitter.DefaultMessageSplitter
import org.springframework.integration.store.MessageStore
import org.springframework.integration.transformer.HeaderFilter
import org.springframework.integration.transformer.MessageTransformingHandler
import org.springframework.integration.transformer.MethodInvokingTransformer
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.support.ChannelInterceptor
import reactor.core.publisher.Flux

import java.util.concurrent.Executor
import java.util.function.Consumer
import java.util.function.Function

/**
 * The Groovy-specific {@link org.springframework.integration.dsl.IntegrationFlowDefinition} wrapper.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see org.springframework.integration.dsl.IntegrationFlowDefinition
 */
@CompileStatic
class GroovyIntegrationFlowDefinition {

	private final IntegrationFlowDefinition delegate

	protected GroovyIntegrationFlowDefinition(IntegrationFlowDefinition delegate) {
		this.delegate = delegate
	}

	/**
	 * Populate an {@link org.springframework.integration.channel.FixedSubscriberChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The provided {@code messageChannelName} is used for the bean registration.
	 * @param messageChannelName the bean name to use.
	 */
	GroovyIntegrationFlowDefinition fixedSubscriberChannel(String messageChannelName = null) {
		this.delegate.fixedSubscriberChannel messageChannelName
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.dsl.support.MessageChannelReference} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The provided {@code messageChannelName} is used for the bean registration
	 * ({@link org.springframework.integration.channel.DirectChannel}), if there is no such a bean
	 * in the application context. Otherwise the existing {@link MessageChannel} bean is used
	 * to wire integration endpoints.
	 * @param messageChannelName the bean name to use.
	 */
	GroovyIntegrationFlowDefinition channel(String messageChannelName) {
		this.delegate.channel messageChannelName
		this
	}

	/**
	 * Populate a {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position using the {@link MessageChannelSpec}
	 * fluent API.
	 * @param messageChannelSpec the {@link MessageChannelSpec} to use.
	 * @see org.springframework.integration.dsl.MessageChannels
	 */
	GroovyIntegrationFlowDefinition channel(MessageChannelSpec messageChannelSpec) {
		this.delegate.channel messageChannelSpec
		this
	}

	/**
	 * Populate the provided {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position.
	 * The {@code messageChannel} can be an existing bean, or fresh instance, in which case
	 * the {@link org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor}
	 * will populate it as a bean with a generated name.
	 * @param messageChannel the {@link MessageChannel} to populate.
	 */
	GroovyIntegrationFlowDefinition channel(MessageChannel messageChannel) {
		this.delegate.channel messageChannel
		this
	}

	/**
	 * Populate a {@link MessageChannel} instance
	 * at the current {@link IntegrationFlow} chain position using the {@link org.springframework.integration.dsl.Channels}
	 * factory fluent API.
	 * @param channels the {@link Function} to use.
	 */
	GroovyIntegrationFlowDefinition channel(
			@DelegatesTo(value = Channels, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.Channels')
					Closure<MessageChannelSpec> channels) {

		Function<Channels, MessageChannelSpec> function =
				{ channelFactory ->
					channels.delegate = channelFactory
					channels.resolveStrategy = Closure.DELEGATE_FIRST
					channels(channelFactory)
				}

		this.delegate.channel function
		this
	}

	/**
	 * The {@link org.springframework.integration.channel.PublishSubscribeChannel} {@link #channel}
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 * @param executor the {@link Executor} to use.
	 * @param publishSubscribeChannelConfigurer the {@link java.util.function.Consumer} to specify
	 * {@link org.springframework.integration.dsl.PublishSubscribeSpec} options including 'subflow' definition.
	 */
	GroovyIntegrationFlowDefinition publishSubscribeChannel(
			Executor executor = null,
			@DelegatesTo(value = PublishSubscribeSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class,
					options = 'org.springframework.integration.dsl.PublishSubscribeSpec')
					Closure<?> publishSubscribeChannelConfigurer) {

		this.delegate.publishSubscribeChannel executor, createConfigurerIfAny(publishSubscribeChannelConfigurer)
		this
	}

	/**
	 * The {@link BroadcastCapableChannel} {@link #channel}
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 * @param broadcastCapableChannel the {@link BroadcastCapableChannel} to subscriber sub-flows to.
	 * @param publishSubscribeChannelConfigurer the {@link Consumer} to specify
	 * {@link org.springframework.integration.dsl.BroadcastPublishSubscribeSpec} 'subflow' definitions.
	 */
	GroovyIntegrationFlowDefinition publishSubscribeChannel(
			BroadcastCapableChannel broadcastCapableChannel,
			@DelegatesTo(value = BroadcastPublishSubscribeSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class,
					options = 'org.springframework.integration.dsl.BroadcastPublishSubscribeSpec')
					Closure<?> publishSubscribeChannelConfigurer) {

		this.delegate.publishSubscribeChannel broadcastCapableChannel,
				createConfigurerIfAny(publishSubscribeChannelConfigurer)
		this
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link ChannelInterceptor} implementation
	 * to the current message channel.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints.
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link ChannelInterceptor}s.
	 * @param flow the {@link IntegrationFlow} for wire-tap subflow as an alternative to the {@code wireTapChannel}.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the
	 * {@link org.springframework.integration.channel.interceptor.WireTap}.
	 */
	GroovyIntegrationFlowDefinition wireTap(
			IntegrationFlow flow,
			@DelegatesTo(value = WireTapSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.WireTapSpec')
					Closure<?> wireTapConfigurer = null) {

		this.delegate.wireTap flow, createConfigurerIfAny(wireTapConfigurer)
		this
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link ChannelInterceptor} implementation
	 * to the current message channel.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints.
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} bean name to wire-tap.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the
	 * {@link org.springframework.integration.channel.interceptor.WireTap}.
	 */
	GroovyIntegrationFlowDefinition wireTap(
			String wireTapChannel,
			@DelegatesTo(value = WireTapSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.WireTapSpec')
					Closure<?> wireTapConfigurer = null) {

		this.delegate.wireTap wireTapChannel, createConfigurerIfAny(wireTapConfigurer)
		this
	}

	/**
	 * Populate the {@code Wire Tap} EI Pattern specific
	 * {@link ChannelInterceptor} implementation
	 * to the current current MessageChannel.
	 * It is useful when an implicit {@link MessageChannel} is used between endpoints.
	 * This method can be used after any {@link #channel} for explicit {@link MessageChannel},
	 * but with the caution do not impact existing {@link ChannelInterceptor}s.
	 * @param wireTapChannel the {@link MessageChannel} to wire-tap.
	 * @param wireTapConfigurer the {@link Consumer} to accept options for the
	 * {@link org.springframework.integration.channel.interceptor.WireTap}.
	 */
	GroovyIntegrationFlowDefinition wireTap(
			MessageChannel wireTapChannel,
			@DelegatesTo(value = WireTapSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.WireTapSpec')
					Closure<?> wireTapConfigurer = null) {

		this.delegate.wireTap wireTapChannel, createConfigurerIfAny(wireTapConfigurer)
		this
	}

	/**
	 * Populate the {@code Control Bus} EI Pattern specific {@link MessageHandler} implementation
	 * at the current {@link IntegrationFlow} chain position.
	 * @param endpointConfigurer the {@link Consumer} to accept integration endpoint options.
	 * @since 6.4
	 * @deprecated in favor of {@link #controlBus}
	 */
	@Deprecated(since = '6.5', forRemoval = true)
	@SuppressWarnings('removal')
	GroovyIntegrationFlowDefinition controlBusOnRegistry(
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		controlBus endpointConfigurer
	}

	/**
	 * Populate the {@code Control Bus} EI Pattern specific {@link MessageHandler} implementation
	 * at the current {@link IntegrationFlow} chain position.
	 * @param endpointConfigurer the {@link Consumer} to accept integration endpoint options.
	 * @see GenericEndpointSpec
	 */
	GroovyIntegrationFlowDefinition controlBus(
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.controlBus createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance for the
	 * {@link org.springframework.integration.handler.MessageProcessor} from provided {@link MessageProcessorSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param transformerConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @see MethodInvokingTransformer
	 * @since 6.2
	 */
	GroovyIntegrationFlowDefinition transform(
			@DelegatesTo(value = TransformerEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.TransformerEndpointSpec')
					Closure<?> transformerConfigurer) {

		this.delegate.transformWith createConfigurerIfAny(transformerConfigurer)
		this
	}

	/**
	 * Populate the {@link MessageTransformingHandler} instance
	 * for the provided {@code payloadType} to convert at runtime.
	 * In addition, accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param payloadType the {@link Class} for expected payload type.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param < P >                                                  the payload type - 'transform to'.
	 */
	<P> GroovyIntegrationFlowDefinition convert(
			Class<P> payloadType,
			@DelegatesTo(value = GenericEndpointSpec<MessageTransformingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.convert payloadType, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.filter.MessageFilter} with
	 * {@link org.springframework.integration.core.MessageSelector} for the provided SpEL expression.
	 * In addition accept options for the integration endpoint using {@link FilterEndpointSpec}
	 * @param expression the SpEL expression.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @see FilterEndpointSpec
	 */
	GroovyIntegrationFlowDefinition filter(
			String expression,
			@DelegatesTo(value = FilterEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.FilterEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.filter expression, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.filter.MessageFilter}
	 * with {@link org.springframework.integration.filter.MethodInvokingSelector} for the
	 * method of the provided service.
	 * @param service the service to use.
	 * @param methodName the method to invoke
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition filter(
			Object service, String methodName = null,
			@DelegatesTo(value = FilterEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.FilterEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.filter service, methodName, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.filter.MessageFilter} with {@link MethodInvokingSelector}
	 * for the {@link MessageProcessor} from
	 * the provided {@link MessageProcessorSpec}.
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition filter(
			MessageProcessorSpec<?> messageProcessorSpec,
			@DelegatesTo(value = FilterEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.FilterEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.filter messageProcessorSpec, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.filter.MessageFilter} with {@link MethodInvokingSelector}
	 * for the provided {@link GenericSelector}.
	 * In addition, accept options for the integration endpoint using {@link FilterEndpointSpec}.
	 * @param expectedType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the selector.
	 * Conversion to this type will be attempted, if necessary.
	 * @param genericSelector the {@link GenericSelector} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param < P >                                                  the source payload type or {@code Message.class}.
	 */
	<P> GroovyIntegrationFlowDefinition filter(
			@DelegatesTo.Target Class<P> expectedType,
			@DelegatesTo(genericTypeIndex = 0, strategy = Closure.DELEGATE_FIRST)
					Closure<Boolean> genericSelector,
			@DelegatesTo(value = FilterEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.FilterEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		GenericSelector<P> lambdaWrapper = payload -> genericSelector(payload)

		this.delegate.filter expectedType, lambdaWrapper, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param beanName the bean name to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition handle(
			String beanName, String methodName,
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.handle beanName, methodName, createConfigurerIfAny(endpointConfigurer)
		this
	}


	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the {@code method} for provided {@code bean} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param service the service object to use.
	 * @param methodName the method to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition handle(
			Object service, String methodName = null,
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.handle service, methodName, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link org.springframework.integration.handler.MethodInvokingMessageProcessor}
	 * to invoke the provided {@link GenericHandler} at runtime.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param expectedType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the handler.
	 * Conversion to this type will be attempted, if necessary.
	 * @param handler the handler to invoke.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param < P >        the payload type to expect or {@code Message.class}.
	 */
	<P> GroovyIntegrationFlowDefinition handle(
			Class<P> expectedType, GenericHandler<P> handler,
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		GenericHandler<P> lambdaWrapper = (payload, headers) -> handler(payload, headers)

		this.delegate.handle expectedType, lambdaWrapper, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the
	 * {@link MessageProcessor} from the provided {@link MessageProcessorSpec}.
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition handle(
			MessageProcessorSpec<?> messageProcessorSpec,
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.handle messageProcessorSpec, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the selected protocol specific
	 * {@link MessageHandler} implementation from {@code Namespace Factory}:
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageHandlerSpec the {@link MessageHandlerSpec} to configure protocol specific
	 * {@link MessageHandler}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param < H >                                                  the {@link MessageHandler} type.
	 */
	<H extends MessageHandler> GroovyIntegrationFlowDefinition handle(
			MessageHandlerSpec<?, H> messageHandlerSpec,
			@DelegatesTo(value = GenericEndpointSpec<H>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.handle messageHandlerSpec, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} for the provided
	 * {@link MessageHandler} implementation.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageHandler the {@link MessageHandler} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param < H >                                                  the {@link MessageHandler} type.
	 */
	<H extends MessageHandler> GroovyIntegrationFlowDefinition handle(
			H messageHandler,
			@DelegatesTo(value = GenericEndpointSpec<H>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.handle messageHandler, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link BridgeHandler} to the current integration flow position.
	 * sed with a Closure expression.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @see GenericEndpointSpec
	 */
	GroovyIntegrationFlowDefinition bridge(
			@DelegatesTo(value = GenericEndpointSpec<BridgeHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.bridge createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.handler.DelayHandler} to the current integration flow position.
	 * The {@link DelayerEndpointSpec#messageGroupId(String)} is required option.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @since 6.2
	 * @see DelayerEndpointSpec
	 */
	GroovyIntegrationFlowDefinition delay(
			@DelegatesTo(value = DelayerEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.DelayerEndpointSpec')
					Closure<?> endpointConfigurer) {

		this.delegate.delay createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.transformer.ContentEnricher}
	 * to the current integration flow position
	 * with provided options.
	 * Used with a Closure expression.
	 * @param enricherConfigurer the {@link Closure} to provide
	 * {@link org.springframework.integration.transformer.ContentEnricher} options.
	 * @see org.springframework.integration.dsl.EnricherSpec
	 */
	GroovyIntegrationFlowDefinition enrich(
			@DelegatesTo(value = EnricherSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.EnricherSpec')
					Closure<?> enricherConfigurer) {

		this.delegate.enrich createConfigurerIfAny(enricherConfigurer)
		this
	}

	/**
	 * Populate a {@link MessageTransformingHandler} for
	 * a {@link org.springframework.integration.transformer.HeaderEnricher}
	 * as the result of provided {@link Consumer}.
	 * @param headerEnricherConfigurer the {@link Consumer} to use.
	 * @see org.springframework.integration.dsl.HeaderEnricherSpec
	 */
	GroovyIntegrationFlowDefinition enrichHeaders(
			@DelegatesTo(value = HeaderEnricherSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.HeaderEnricherSpec')
					Closure<?> enricherConfigurer) {

		this.delegate.enrichHeaders createConfigurerIfAny(enricherConfigurer)
		this
	}

	/**
	 * Populate the {@link DefaultMessageSplitter} with default options to the current integration flow position.
	 */
	GroovyIntegrationFlowDefinition split() {
		this.delegate.split()
		this
	}

	/**
	 * Populate the {@link DefaultMessageSplitter} with provided options
	 * to the current integration flow position.
	 * Used with a Closure expression (optional).
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options
	 * and for {@link DefaultMessageSplitter}.
	 * @since 6.2
	 * @see SplitterSpec
	 */
	GroovyIntegrationFlowDefinition splitWith(
			@DelegatesTo(value = SplitterSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.SplitterSpec')
					Closure<?> splitConfigurer) {

		this.delegate.splitWith createConfigurerIfAny(splitConfigurer)
		this
	}

	/**
	 * Populate {@link HeaderFilter} based on the options from a {@link HeaderFilterSpec}.
	 * @param endpointConfigurer the {@link Consumer} to provide {@link HeaderFilter} and its endpoint options.
	 * @see HeaderFilterSpec
	 * @since 6.2
	 */
	GroovyIntegrationFlowDefinition headerFilter(
			@DelegatesTo(value = HeaderFilterSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.HeaderFilterSpec')
					Closure<?> headerFilterConfigurer) {

		this.delegate.headerFilter createConfigurerIfAny(headerFilterConfigurer)
		this
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the
	 * {@link org.springframework.integration.transformer.ClaimCheckInTransformer} with provided {@link MessageStore}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @see GenericEndpointSpec
	 */
	GroovyIntegrationFlowDefinition claimCheckIn(
			MessageStore messageStore,
			@DelegatesTo(value = GenericEndpointSpec<MessageTransformingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.claimCheckIn messageStore, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate the {@link MessageTransformingHandler} for the
	 * {@link org.springframework.integration.transformer.ClaimCheckOutTransformer}
	 * with provided {@link MessageStore} and {@code removeMessage} flag.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param messageStore the {@link MessageStore} to use.
	 * @param removeMessage the removeMessage boolean flag.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @see GenericEndpointSpec* @see org.springframework.integration.transformer.ClaimCheckOutTransformer#setRemoveMessage(boolean)
	 */
	GroovyIntegrationFlowDefinition claimCheckOut(
			MessageStore messageStore, boolean removeMessage = false,
			@DelegatesTo(value = GenericEndpointSpec<MessageTransformingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.claimCheckOut messageStore, removeMessage, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate the
	 * {@link org.springframework.integration.aggregator.ResequencingMessageHandler} with
	 * provided options from {@link org.springframework.integration.dsl.ResequencerSpec}.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Used with a Closure expression (optional).
	 * @param resequencer the {@link Consumer} to provide
	 * {@link org.springframework.integration.aggregator.ResequencingMessageHandler} options.
	 * @see org.springframework.integration.dsl.ResequencerSpec
	 */
	GroovyIntegrationFlowDefinition resequence(
			@DelegatesTo(value = ResequencerSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.ResequencerSpec')
					Closure<?> resequencer = null) {

		this.delegate.resequence createConfigurerIfAny(resequencer)
		this
	}

	/**
	 * A short-cut for the {@code aggregate((aggregator) -> aggregator.processor(aggregatorProcessor))}.
	 * @param aggregatorProcessor the POJO representing aggregation strategies.
	 * @see org.springframework.integration.dsl.AggregatorSpec
	 */
	GroovyIntegrationFlowDefinition aggregate(Object aggregatorProcessor) {
		this.delegate.aggregate aggregatorProcessor
		this
	}

	/**
	 * Populate the {@link org.springframework.integration.aggregator.AggregatingMessageHandler}
	 * with provided options from {@link org.springframework.integration.dsl.AggregatorSpec}.
	 * In addition, accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * Used with a Closure expression (optional).
	 * @param aggregator the {@link Consumer} to provide
	 * {@link org.springframework.integration.aggregator.AggregatingMessageHandler} options.
	 */
	GroovyIntegrationFlowDefinition aggregate(
			@DelegatesTo(value = AggregatorSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.AggregatorSpec')
					Closure<?> aggregator = null) {

		this.delegate.aggregate createConfigurerIfAny(aggregator)
		this
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided bean and its method
	 * with provided options from {@link org.springframework.integration.dsl.RouterSpec}.
	 * @param beanName the bean to use.
	 * @param method the method to invoke at runtime.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 */
	GroovyIntegrationFlowDefinition route(
			String beanName, String method,
			@DelegatesTo(value = RouterSpec<Object, MethodInvokingRouter>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.RouterSpec')
					Closure<?> routerConfigurer = null) {

		this.delegate.route beanName, method, createConfigurerIfAny(routerConfigurer)
		this
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the method
	 * of the provided service and its method with provided options from {@link org.springframework.integration.dsl.RouterSpec}.
	 * @param service the service to use.
	 * @param methodName the method to invoke.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 */
	GroovyIntegrationFlowDefinition route(
			Object service, String methodName = null,
			@DelegatesTo(value = RouterSpec<Object, MethodInvokingRouter>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.RouterSpec')
					Closure<?> routerConfigurer = null) {

		this.delegate.route service, methodName, createConfigurerIfAny(routerConfigurer)
		this
	}

	/**
	 * Populate the {@link ExpressionEvaluatingRouter} for provided SpEL expression
	 * with provided options from {@link org.springframework.integration.dsl.RouterSpec}.
	 * @param expression the expression to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link ExpressionEvaluatingRouter} options.
	 * @param < T >                                                  the target result type.
	 */
	<T> GroovyIntegrationFlowDefinition route(
			String expression,
			@DelegatesTo(value = RouterSpec<T, ExpressionEvaluatingRouter>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.RouterSpec')
					Closure<?> routerConfigurer = null) {

		this.delegate.route expression, createConfigurerIfAny(routerConfigurer)
		this
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for provided {@link Function}
	 * and payload type and options from {@link org.springframework.integration.dsl.RouterSpec}.
	 * In addition, accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param expectedType the {@link Class} for expected payload type. It can also be
	 * {@code Message.class} if you wish to access the entire message in the router.
	 * Conversion to this type will be attempted, if necessary.
	 * @param router the {@link Function} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 * @param < P >                                                  the source payload type or {@code Message.class}.
	 * @param < T >                                                  the target result type.
	 */
	<P, T> GroovyIntegrationFlowDefinition route(
			Class<P> expectedType, Function<P, T> router,
			@DelegatesTo(value = RouterSpec<T, MethodInvokingRouter>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.RouterSpec')
					Closure<?> routerConfigurer = null) {

		Function<P, T> lambdaWrapper = payload -> router(payload)

		this.delegate.route expectedType, lambdaWrapper, createConfigurerIfAny(routerConfigurer)
		this
	}

	/**
	 * Populate the {@link MethodInvokingRouter} for the
	 * {@link MessageProcessor}
	 * from the provided {@link MessageProcessorSpec} with default options.
	 * @param messageProcessorSpec the {@link MessageProcessorSpec} to use.
	 * @param routerConfigurer the {@link Consumer} to provide {@link MethodInvokingRouter} options.
	 */
	GroovyIntegrationFlowDefinition route(
			MessageProcessorSpec<?> messageProcessorSpec,
			@DelegatesTo(value = RouterSpec<Object, MethodInvokingRouter>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.RouterSpec')
					Closure<?> routerConfigurer = null) {

		this.delegate.route messageProcessorSpec, createConfigurerIfAny(routerConfigurer)
		this
	}

	/**
	 * Populate the {@link org.springframework.integration.router.RecipientListRouter}
	 * with options from the {@link org.springframework.integration.dsl.RecipientListRouterSpec}.
	 * Typically used with a Closure expression.
	 * @param routerConfigurer the {@link Consumer} to provide
	 * {@link org.springframework.integration.router.RecipientListRouter} options.
	 */
	GroovyIntegrationFlowDefinition routeToRecipients(
			@DelegatesTo(value = RecipientListRouterSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class,
					options = 'org.springframework.integration.dsl.RecipientListRouterSpec')
					Closure<?> routerConfigurer = null) {

		this.delegate.routeToRecipients createConfigurerIfAny(routerConfigurer)
		this
	}

	/**
	 * Populate the {@link ErrorMessageExceptionTypeRouter} with options from the {@link RouterSpec}.
	 * Typically used with a Closure expression.
	 * @param routerConfigurer the {@link Consumer} to provide {@link ErrorMessageExceptionTypeRouter} options.
	 * @see ErrorMessageExceptionTypeRouter
	 */
	GroovyIntegrationFlowDefinition routeByException(
			@DelegatesTo(value = RouterSpec<Class<? extends Throwable>, ErrorMessageExceptionTypeRouter>,
					strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.RouterSpec')
					Closure<?> routerConfigurer = null) {

		this.delegate.routeByException createConfigurerIfAny(routerConfigurer)
		this
	}

	/**
	 * Populate the provided {@link AbstractMessageRouter} implementation to the
	 * current integration flow position.
	 * In addition accept options for the integration endpoint using {@link GenericEndpointSpec}.
	 * @param router the {@link AbstractMessageRouter} to populate.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 * @param <R >                                                  the {@link AbstractMessageRouter} type.
	 */
	<R extends AbstractMessageRouter> GroovyIntegrationFlowDefinition route(
			R router,
			@DelegatesTo(value = GenericEndpointSpec<R>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.route router, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code requestChannel} to send a request with options from
	 * {@link org.springframework.integration.dsl.GatewayEndpointSpec}. Uses
	 * {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy on the
	 * background.
	 * @param requestChannel the {@link MessageChannel} bean name.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint
	 * options.
	 */
	GroovyIntegrationFlowDefinition gateway(
			String requestChannel,
			@DelegatesTo(value = GatewayEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GatewayEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.gateway requestChannel, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code requestChannel} to send a request with options from
	 * {@link GatewayEndpointSpec}. Uses
	 * {@link org.springframework.integration.gateway.RequestReplyExchanger} Proxy on the
	 * background.
	 * @param requestChannel the {@link MessageChannel} to use.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint
	 * options.
	 */
	GroovyIntegrationFlowDefinition gateway(
			MessageChannel requestChannel,
			@DelegatesTo(value = GatewayEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GatewayEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.gateway requestChannel, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate the "artificial"
	 * {@link org.springframework.integration.gateway.GatewayMessageHandler} for the
	 * provided {@code subflow} with options from {@link GatewayEndpointSpec}.
	 * Typically used with a Closure expression.
	 * @param flow the {@link IntegrationFlow} to to send a request message and wait for reply.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition gateway(
			IntegrationFlow flow,
			@DelegatesTo(value = GatewayEndpointSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.GatewayEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.gateway flow, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link WireTap} for the current message channel
	 * with the {@link LoggingHandler} subscriber for provided {@link LoggingHandler.Level}
	 * logging level and {@code org.springframework.integration.handler.LoggingHandler}
	 * as a default logging category.
	 * <p> The full request {@link Message} will be logged.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * @param level the {@link LoggingHandler.Level}.
	 */
	GroovyIntegrationFlowDefinition log(LoggingHandler.Level level = LoggingHandler.Level.INFO,
										String category, String logExpression = null) {

		if (logExpression) {
			this.delegate.log level, category, logExpression
		} else {
			this.delegate.log level, category
		}
		this
	}

	/**
	 * Populate a {@link WireTap} for the current message channel
	 * with the {@link LoggingHandler} subscriber for the provided
	 * {@link LoggingHandler.Level} logging level, logging category
	 * and {@link Function} for the log message.
	 * <p> When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 * @param level the {@link LoggingHandler.Level}.
	 * @param category the logging category.
	 * @param function the function to evaluate logger message at runtime
	 * @param < P >    the expected payload type against the request {@link Message}.
	 */
	<P> GroovyIntegrationFlowDefinition log(
			LoggingHandler.Level level = LoggingHandler.Level.INFO,
			String category = null,
			Function<Message<P>, Object> function) {

		this.delegate.log level, category, function
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.scattergather.ScatterGatherHandler}
	 * to the current integration flow position
	 * based on the provided {@link MessageChannel} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterChannel the {@link MessageChannel} for scatting requests.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * Can be {@code null}.
	 * @param scatterGather the {@link Consumer} for {@link ScatterGatherSpec} to configure
	 * {@link org.springframework.integration.scattergather.ScatterGatherHandler} and its endpoint. Can be {@code null}.
	 */
	GroovyIntegrationFlowDefinition scatterGather(
			MessageChannel scatterChannel,
			@DelegatesTo(value = AggregatorSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.AggregatorSpec')
					Closure<?> gatherer = null,
			@DelegatesTo(value = ScatterGatherSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.ScatterGatherSpec')
					Closure<?> scatterGather = null) {

		this.delegate.scatterGather scatterChannel, createConfigurerIfAny(gatherer),
				createConfigurerIfAny(scatterGather)
		this
	}

	/**
	 * Populate a {@link ScatterGatherHandler} to the current integration flow position
	 * based on the provided {@link RecipientListRouterSpec} for scattering function
	 * and {@link AggregatorSpec} for gathering function.
	 * @param scatterer the {@link Consumer} for {@link RecipientListRouterSpec} to configure scatterer.
	 * @param gatherer the {@link Consumer} for {@link AggregatorSpec} to configure gatherer.
	 * @param scatterGather the {@link Consumer} for {@link ScatterGatherSpec} to configure
	 * {@link ScatterGatherHandler} and its endpoint. Can be {@code null}.
	 */
	GroovyIntegrationFlowDefinition scatterGather(
			@DelegatesTo(value = RecipientListRouterSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class,
					options = 'org.springframework.integration.dsl.RecipientListRouterSpec')
					Closure<?> scatterer,
			@DelegatesTo(value = AggregatorSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.AggregatorSpec')
					Closure<?> gatherer = null,
			@DelegatesTo(value = ScatterGatherSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class, options = 'org.springframework.integration.dsl.ScatterGatherSpec')
					Closure<?> scatterGather = null) {

		this.delegate.scatterGather createConfigurerIfAny(scatterer), createConfigurerIfAny(gatherer),
				createConfigurerIfAny(scatterGather)
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.aggregator.BarrierMessageHandler}
	 * instance for provided timeout and options from {@link org.springframework.integration.dsl.BarrierSpec} and endpoint
	 * options from {@link GenericEndpointSpec}.
	 * @param timeout the timeout in milliseconds.
	 * @param barrierConfigurer the {@link Consumer} to provide
	 * {@link org.springframework.integration.aggregator.BarrierMessageHandler} options.
	 */
	GroovyIntegrationFlowDefinition barrier(long timeout,
											@DelegatesTo(value = BarrierSpec, strategy = Closure.DELEGATE_FIRST)
											@ClosureParams(value = SimpleType.class,
													options = 'org.springframework.integration.dsl.BarrierSpec')
													Closure<?> barrierConfigurer = null) {

		this.delegate.barrier timeout, createConfigurerIfAny(barrierConfigurer)
		this
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}
	 * and endpoint options from {@link GenericEndpointSpec}.
	 * @param triggerActionId the {@link MessageTriggerAction} bean id.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition trigger(
			String triggerActionId,
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class,
					options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.trigger triggerActionId, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Populate a {@link ServiceActivatingHandler} instance to perform {@link MessageTriggerAction}
	 * and endpoint options from {@link GenericEndpointSpec}.
	 * @param triggerAction the {@link MessageTriggerAction}.
	 * @param endpointConfigurer the {@link Consumer} to provide integration endpoint options.
	 */
	GroovyIntegrationFlowDefinition trigger(
			MessageTriggerAction triggerAction,
			@DelegatesTo(value = GenericEndpointSpec<ServiceActivatingHandler>, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType.class,
					options = 'org.springframework.integration.dsl.GenericEndpointSpec')
					Closure<?> endpointConfigurer = null) {

		this.delegate.trigger triggerAction, createConfigurerIfAny(endpointConfigurer)
		this
	}

	/**
	 * Add one or more {@link ChannelInterceptor} implementations
	 * to the current {@link MessageChannel}, in the given order, after any interceptors already registered.
	 * @param interceptorArray one or more {@link ChannelInterceptor}s.
	 */
	GroovyIntegrationFlowDefinition intercept(ChannelInterceptor... interceptorArray) {
		this.delegate.intercept interceptorArray
		this
	}

	/**
	 * Populate a {@link org.springframework.integration.channel.FluxMessageChannel}
	 * to start a reactive processing for upstream data,
	 * wrap it to a {@link Flux}, apply provided {@link Function} via {@link Flux#transform(Function)}
	 * and emit the result to one more
	 * {@link org.springframework.integration.channel.FluxMessageChannel}, subscribed in the downstream flow.
	 * @param fluxFunction the {@link Function} to process data reactive manner.
	 * @param < I >                                                  the input payload type.
	 * @param < O >                                                  the output type.
	 */
	<I, O> GroovyIntegrationFlowDefinition fluxTransform(
			Function<? super Flux<Message<I>>, ? extends Publisher<O>> fluxFunction) {

		this.delegate.<I, O> fluxTransform fluxFunction
		this
	}

	protected static <T> Consumer<T> createConfigurerIfAny(
			@DelegatesTo(strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {

		if (closure) {
			return {
				closure.delegate = it
				closure.resolveStrategy = Closure.DELEGATE_FIRST
				closure(it)
			} as Consumer<T>
		}
		null
	}

}
