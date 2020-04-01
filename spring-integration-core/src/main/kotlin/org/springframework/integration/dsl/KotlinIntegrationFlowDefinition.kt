/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.dsl

import org.reactivestreams.Publisher
import org.springframework.expression.Expression
import org.springframework.integration.aggregator.AggregatingMessageHandler
import org.springframework.integration.channel.BroadcastCapableChannel
import org.springframework.integration.channel.FluxMessageChannel
import org.springframework.integration.channel.interceptor.WireTap
import org.springframework.integration.core.MessageSelector
import org.springframework.integration.dsl.support.MessageChannelReference
import org.springframework.integration.filter.MessageFilter
import org.springframework.integration.filter.MethodInvokingSelector
import org.springframework.integration.handler.BridgeHandler
import org.springframework.integration.handler.DelayHandler
import org.springframework.integration.handler.GenericHandler
import org.springframework.integration.handler.LoggingHandler
import org.springframework.integration.handler.MessageProcessor
import org.springframework.integration.handler.MessageTriggerAction
import org.springframework.integration.handler.ServiceActivatingHandler
import org.springframework.integration.router.AbstractMessageRouter
import org.springframework.integration.router.ErrorMessageExceptionTypeRouter
import org.springframework.integration.router.ExpressionEvaluatingRouter
import org.springframework.integration.router.MethodInvokingRouter
import org.springframework.integration.router.RecipientListRouter
import org.springframework.integration.scattergather.ScatterGatherHandler
import org.springframework.integration.splitter.AbstractMessageSplitter
import org.springframework.integration.splitter.DefaultMessageSplitter
import org.springframework.integration.splitter.ExpressionEvaluatingSplitter
import org.springframework.integration.splitter.MethodInvokingSplitter
import org.springframework.integration.store.MessageStore
import org.springframework.integration.support.MapBuilder
import org.springframework.integration.transformer.ClaimCheckInTransformer
import org.springframework.integration.transformer.ClaimCheckOutTransformer
import org.springframework.integration.transformer.HeaderFilter
import org.springframework.integration.transformer.MessageTransformingHandler
import org.springframework.integration.transformer.MethodInvokingTransformer
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.MessageHeaders
import reactor.core.publisher.Flux
import java.util.function.Consumer

/**
 * An [IntegrationFlowDefinition] wrapped for Kotlin DSL.
 *
 * @property delegate the [IntegrationFlowDefinition] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
class KotlinIntegrationFlowDefinition(@PublishedApi internal val delegate: IntegrationFlowDefinition<*>) {

	/**
	 * Inline function for [IntegrationFlowDefinition.convert] providing a `convert<MyType>()` variant
	 * with reified generic type.
	 */
	inline fun <reified T> convert(
			crossinline configurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit = {}) {

		this.delegate.convert(T::class.java) { configurer(it) }
	}

	/**
	 * Inline function for [IntegrationFlowDefinition.transform] providing a `transform<MyTypeIn, MyTypeOut>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P> transform(crossinline function: (P) -> Any) {
		this.delegate.transform(P::class.java) { function(it) }
	}

	/**
	 * Inline function for [IntegrationFlowDefinition.transform] providing a `transform<MyTypeIn, MyTypeOut>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P> transform(
			crossinline function: (P) -> Any,
			crossinline configurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit) {

		this.delegate.transform(P::class.java, { function(it) }) { configurer(it) }
	}

	/**
	 * Inline function for [IntegrationFlowDefinition.split] providing a `split<MyTypeIn>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P> split(crossinline function: (P) -> Any) {
		this.delegate.split(P::class.java) { function(it) }
	}


	/**
	 * Inline function for [IntegrationFlowDefinition.split] providing a `split<MyTypeIn>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P> split(
			crossinline function: (P) -> Any,
			crossinline configurer: KotlinSplitterEndpointSpec<MethodInvokingSplitter>.() -> Unit) {

		this.delegate.split(P::class.java, { function(it) }) { configurer(KotlinSplitterEndpointSpec(it)) }
	}

	/**
	 * Inline function for [IntegrationFlowDefinition.filter] providing a `filter<MyTypeIn>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P> filter(crossinline function: (P) -> Boolean) {
		this.delegate.filter(P::class.java) { function(it) }
	}

	/**
	 * Inline function for [IntegrationFlowDefinition.filter] providing a `filter<MyTypeIn>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P> filter(
			crossinline function: (P) -> Boolean,
			crossinline filterConfigurer: KotlinFilterEndpointSpec.() -> Unit) {

		this.delegate.filter(P::class.java, { function(it) }) { filterConfigurer(KotlinFilterEndpointSpec(it)) }
	}


	/**
	 * Inline function for [IntegrationFlowDefinition.filter] providing a `filter<MyTypeIn>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P> route(crossinline function: (P) -> Any?) {
		route(function) { }
	}

	/**
	 * Inline function for [IntegrationFlowDefinition.filter] providing a `filter<MyTypeIn>()` variant
	 * with reified generic type.
	 */
	inline fun <reified P, T> route(
			crossinline function: (P) -> T,
			crossinline configurer: KotlinRouterSpec<T, MethodInvokingRouter>.() -> Unit) {

		this.delegate.route(P::class.java, { function(it) }) { configurer(KotlinRouterSpec(it)) }
	}

	/**
	 * Populate an [org.springframework.integration.channel.FixedSubscriberChannel] instance
	 * at the current [IntegrationFlow] chain position.
	 * The provided `messageChannelName` is used for the bean registration.
	 */
	fun fixedSubscriberChannel(messageChannelName: String? = null) {
		this.delegate.fixedSubscriberChannel(messageChannelName)
	}

	/**
	 * Populate a [MessageChannelReference] instance
	 * at the current [IntegrationFlow] chain position.
	 * The provided `messageChannelName` is used for the bean registration
	 * ([org.springframework.integration.channel.DirectChannel]), if there is no such a bean
	 * in the application context. Otherwise the existing [MessageChannel] bean is used
	 * to wire integration endpoints.
	 */
	fun channel(messageChannelName: String) {
		this.delegate.channel(messageChannelName)
	}

	/**
	 * Populate a [MessageChannel] instance
	 * at the current [IntegrationFlow] chain position using the [MessageChannelSpec]
	 * fluent API.
	 */
	fun channel(messageChannelSpec: MessageChannelSpec<*, *>) {
		this.delegate.channel(messageChannelSpec)
	}

	/**
	 * Populate the provided [MessageChannel] instance
	 * at the current [IntegrationFlow] chain position.
	 * The `messageChannel` can be an existing bean, or fresh instance, in which case
	 * the [org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor]
	 * will populate it as a bean with a generated name.
	 */
	fun channel(messageChannel: MessageChannel) {
		this.delegate.channel(messageChannel)
	}

	/**
	 * Populate a [MessageChannel] instance
	 * at the current [IntegrationFlow] chain position using the [Channels]
	 * factory fluent API.
	 */
	fun channel(channels: Channels.() -> MessageChannelSpec<*, *>) {
		this.delegate.channel(channels)
	}

	/**
	 * The [org.springframework.integration.channel.BroadcastCapableChannel] `channel()`
	 * method specific implementation to allow the use of the 'subflow' subscriber capability.
	 */
	fun publishSubscribe(broadcastCapableChannel: BroadcastCapableChannel,
						 vararg subscribeSubFlows: KotlinIntegrationFlowDefinition.() -> Unit) {

		val publishSubscribeChannelConfigurer =
				Consumer<BroadcastPublishSubscribeSpec> { spec ->
					subscribeSubFlows.forEach { subFlow ->
						spec.subscribe { subFlow(KotlinIntegrationFlowDefinition(it)) }
					}
				}
		this.delegate.publishSubscribeChannel(broadcastCapableChannel, publishSubscribeChannelConfigurer)
	}

	/**
	 * Populate the `Wire Tap` EI Pattern specific
	 * [org.springframework.messaging.support.ChannelInterceptor] implementation
	 * to the current channel.
	 * This method can be used after any `channel()` for explicit [MessageChannel],
	 * but with the caution do not impact existing [org.springframework.messaging.support.ChannelInterceptor]s.
	 */
	fun wireTap(flow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.wireTap(IntegrationFlow { flow(KotlinIntegrationFlowDefinition(it)) })
	}

	/**
	 * Populate the `Wire Tap` EI Pattern specific
	 * [org.springframework.messaging.support.ChannelInterceptor] implementation
	 * to the current channel.
	 * This method can be used after any `channel()` for explicit [MessageChannel],
	 * but with the caution do not impact existing [org.springframework.messaging.support.ChannelInterceptor]s.
	 */
	fun wireTap(wireTapConfigurer: WireTapSpec.() -> Unit, flow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.wireTap(
				IntegrationFlow { flow(KotlinIntegrationFlowDefinition(it)) },
				Consumer(wireTapConfigurer))
	}

	/**
	 * Populate the `Wire Tap` EI Pattern specific
	 * [org.springframework.messaging.support.ChannelInterceptor] implementation
	 * to the current channel.
	 * This method can be used after any `channel()` for explicit [MessageChannel],
	 * but with the caution do not impact existing [org.springframework.messaging.support.ChannelInterceptor]s.
	 */
	fun wireTap(wireTapChannel: String, wireTapConfigurer: WireTapSpec.() -> Unit = {}) {
		this.delegate.wireTap(wireTapChannel, wireTapConfigurer)
	}

	/**
	 * Populate the `Wire Tap` EI Pattern specific
	 * [org.springframework.messaging.support.ChannelInterceptor] implementation
	 * to the current channel.
	 * This method can be used after any `channel()` for explicit [MessageChannel],
	 * but with the caution do not impact existing [org.springframework.messaging.support.ChannelInterceptor]s.
	 */
	fun wireTap(wireTapChannel: MessageChannel, wireTapConfigurer: WireTapSpec.() -> Unit = {}) {
		this.delegate.wireTap(wireTapChannel, Consumer(wireTapConfigurer))
	}

	/**
	 * Populate the `Wire Tap` EI Pattern specific
	 * [org.springframework.messaging.support.ChannelInterceptor] implementation
	 * to the current channel.
	 * This method can be used after any `channel()` for explicit [MessageChannel],
	 * but with the caution do not impact existing [org.springframework.messaging.support.ChannelInterceptor]s.
	 */
	fun wireTap(wireTapSpec: WireTapSpec) {
		this.delegate.wireTap(wireTapSpec)
	}

	/**
	 * Populate the `Control Bus` EI Pattern specific [MessageHandler] implementation
	 * at the current [IntegrationFlow] chain position.
	 */
	fun controlBus(endpointConfigurer: GenericEndpointSpec<ServiceActivatingHandler>.() -> Unit = {}) {
		this.delegate.controlBus(endpointConfigurer)
	}

	/**
	 * Populate the `Transformer` EI Pattern specific [MessageHandler] implementation
	 * for the SpEL [Expression].
	 */
	fun transform(expression: String,
				  endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit = {}) {

		this.delegate.transform(expression, endpointConfigurer)
	}

	/**
	 * Populate the `MessageTransformingHandler` for the [MethodInvokingTransformer]
	 * to invoke the service method at runtime.
	 */
	fun transform(service: Any, methodName: String? = null) {
		this.delegate.transform(service, methodName)
	}

	/**
	 * Populate the `MessageTransformingHandler` for the [MethodInvokingTransformer]
	 * to invoke the service method at runtime.
	 */
	fun transform(service: Any, methodName: String?,
				  endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit) {

		this.delegate.transform(service, methodName, endpointConfigurer)
	}

	/**
	 * Populate the [MessageTransformingHandler] instance for the
	 * [org.springframework.integration.handler.MessageProcessor] from provided [MessageProcessorSpec].
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun transform(messageProcessorSpec: MessageProcessorSpec<*>,
				  endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit = {}) {

		this.delegate.transform(messageProcessorSpec, endpointConfigurer)
	}

	/**
	 * Populate a [MessageFilter] with [MessageSelector] for the provided SpEL expression.
	 * In addition accept options for the integration endpoint using [KotlinFilterEndpointSpec]:
	 */
	fun filter(expression: String, filterConfigurer: KotlinFilterEndpointSpec.() -> Unit = {}) {
		this.delegate.filter(expression) { filterConfigurer(KotlinFilterEndpointSpec(it)) }
	}

	/**
	 * Populate a [MessageFilter] with [MethodInvokingSelector] for the
	 * method of the provided service.
	 */
	fun filter(service: Any, methodName: String? = null) {
		this.delegate.filter(service, methodName)
	}

	/**
	 * Populate a [MessageFilter] with [MethodInvokingSelector] for the
	 * method of the provided service.
	 */
	fun filter(service: Any, methodName: String?, filterConfigurer: KotlinFilterEndpointSpec.() -> Unit) {
		this.delegate.filter(service, methodName) { filterConfigurer(KotlinFilterEndpointSpec(it)) }
	}

	/**
	 * Populate a [MessageFilter] with [MethodInvokingSelector]
	 * for the [MessageProcessor] from
	 * the provided [MessageProcessorSpec].
	 * In addition accept options for the integration endpoint using [KotlinFilterEndpointSpec].
	 */
	fun filter(messageProcessorSpec: MessageProcessorSpec<*>,
			   filterConfigurer: KotlinFilterEndpointSpec.() -> Unit = {}) {
		this.delegate.filter(messageProcessorSpec) { filterConfigurer(KotlinFilterEndpointSpec(it)) }
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the selected protocol specific
	 * [MessageHandler] implementation from `Namespace Factory`:
	 */
	fun <H : MessageHandler?> handle(messageHandlerSpec: MessageHandlerSpec<*, H>) {
		this.delegate.handle(messageHandlerSpec)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the provided
	 * [MessageHandler] implementation.
	 */
	fun handle(messageHandler: MessageHandler) {
		this.delegate.handle(messageHandler)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the
	 * [org.springframework.integration.handler.MethodInvokingMessageProcessor]
	 * to invoke the `method` for provided `bean` at runtime.
	 */
	fun handle(beanName: String, methodName: String? = null) {
		this.delegate.handle(beanName, methodName)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the
	 * [org.springframework.integration.handler.MethodInvokingMessageProcessor]
	 * to invoke the `method` for provided `bean` at runtime.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun handle(beanName: String, methodName: String?,
			   endpointConfigurer: GenericEndpointSpec<ServiceActivatingHandler>.() -> Unit) {

		this.delegate.handle(beanName, methodName, endpointConfigurer)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the
	 * [org.springframework.integration.handler.MethodInvokingMessageProcessor]
	 * to invoke the `method` for provided `bean` at runtime.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun handle(service: Any, methodName: String? = null) {
		this.delegate.handle(service, methodName)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the
	 * [org.springframework.integration.handler.MethodInvokingMessageProcessor]
	 * to invoke the `method` for provided `bean` at runtime.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun handle(service: Any, methodName: String?,
			   endpointConfigurer: GenericEndpointSpec<ServiceActivatingHandler>.() -> Unit) {

		this.delegate.handle(service, methodName, endpointConfigurer)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the
	 * [org.springframework.integration.handler.MethodInvokingMessageProcessor]
	 * to invoke the provided [GenericHandler] at runtime.
	 */
	inline fun <reified P> handle(crossinline handler: (P, MessageHeaders) -> Any) {
		this.delegate.handle(P::class.java) { p, h -> handler(p, h) }
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the
	 * [org.springframework.integration.handler.MethodInvokingMessageProcessor]
	 * to invoke the provided [GenericHandler] at runtime.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	inline fun <reified P> handle(
			crossinline handler: (P, MessageHeaders) -> Any,
			crossinline endpointConfigurer: GenericEndpointSpec<ServiceActivatingHandler>.() -> Unit) {

		this.delegate.handle(P::class.java, { p, h -> handler(p, h) }) { endpointConfigurer(it) }
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the [MessageProcessor] from the provided [MessageProcessorSpec].
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun handle(messageProcessorSpec: MessageProcessorSpec<*>,
			   endpointConfigurer: GenericEndpointSpec<ServiceActivatingHandler>.() -> Unit = {}) {

		this.delegate.handle(messageProcessorSpec, endpointConfigurer)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the selected protocol specific
	 * [MessageHandler] implementation from `Namespace Factory`:
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun <H : MessageHandler> handle(messageHandlerSpec: MessageHandlerSpec<*, H>,
									endpointConfigurer: GenericEndpointSpec<H>.() -> Unit = {}) {

		this.delegate.handle(messageHandlerSpec, endpointConfigurer)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the provided
	 * [MessageHandler] lambda.
	 */
	fun handle(messageHandler: (Message<*>) -> Unit) {
		this.delegate.handle(MessageHandler { messageHandler(it) })
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the provided
	 * [MessageHandler] lambda.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun handle(messageHandler: (Message<*>) -> Unit,
			   endpointConfigurer: GenericEndpointSpec<MessageHandler>.() -> Unit) {

		this.delegate.handle(MessageHandler { messageHandler(it) }, endpointConfigurer)
	}

	/**
	 * Populate a [ServiceActivatingHandler] for the provided
	 * [MessageHandler] implementation.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun <H : MessageHandler> handle(messageHandler: H, endpointConfigurer: GenericEndpointSpec<H>.() -> Unit = {}) {
		this.delegate.handle(messageHandler, endpointConfigurer)
	}

	/**
	 * Populate a [BridgeHandler] to the current integration flow position.
	 */
	fun bridge(endpointConfigurer: GenericEndpointSpec<BridgeHandler>.() -> Unit = {}) {
		this.delegate.bridge(endpointConfigurer)
	}

	/**
	 * Populate a [DelayHandler] to the current integration flow position.
	 */
	fun delay(groupId: String, endpointConfigurer: DelayerEndpointSpec.() -> Unit = {}) {
		this.delegate.delay(groupId, endpointConfigurer)
	}

	/**
	 * Populate a [org.springframework.integration.transformer.ContentEnricher]
	 * to the current integration flow position
	 * with provided options.
	 */
	fun enrich(enricherConfigurer: KotlinEnricherSpec.() -> Unit) {
		this.delegate.enrich { enricherConfigurer(KotlinEnricherSpec(it)) }
	}

	/**
	 * Populate a [MessageTransformingHandler] for
	 * a [org.springframework.integration.transformer.HeaderEnricher]
	 * using header values from provided [MapBuilder].
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun enrichHeaders(headers: MapBuilder<*, String, Any>,
					  endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit = {}) {

		this.delegate.enrichHeaders(headers, endpointConfigurer)
	}

	/**
	 * Accept a [Map] of values to be used for the
	 * [Message] header enrichment.
	 * `values` can apply an [Expression]
	 * to be evaluated against a request [Message].
	 */
	fun enrichHeaders(headers: Map<String, Any>,
					  endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit = {}) {

		this.delegate.enrichHeaders(headers, endpointConfigurer)
	}

	/**
	 * Populate a [MessageTransformingHandler] for
	 * a [org.springframework.integration.transformer.HeaderEnricher]
	 * as the result of provided consumer.
	 */
	fun enrichHeaders(headerEnricherConfigurer: HeaderEnricherSpec.() -> Unit) {
		this.delegate.enrichHeaders(headerEnricherConfigurer)
	}

	/**
	 * Populate the [DefaultMessageSplitter] with provided options
	 * to the current integration flow position.
	 */
	fun split() {
		this.delegate.split()
	}

	/**
	 * Populate the [ExpressionEvaluatingSplitter] with provided
	 * SpEL expression.
	 */
	fun split(expression: String,
			  endpointConfigurer: KotlinSplitterEndpointSpec<ExpressionEvaluatingSplitter>.() -> Unit = {}) {

		this.delegate.split(expression) { endpointConfigurer(KotlinSplitterEndpointSpec(it)) }
	}

	/**
	 * Populate the [MethodInvokingSplitter] to evaluate the provided
	 * `method` of the `service` at runtime.
	 */
	fun split(service: Any, methodName: String? = null) {
		this.delegate.split(service, methodName)
	}

	/**
	 * Populate the [MethodInvokingSplitter] to evaluate the provided
	 * `method` of the `bean` at runtime.
	 * In addition accept options for the integration endpoint using [KotlinSplitterEndpointSpec].
	 */
	fun split(service: Any, methodName: String?,
			  splitterConfigurer: KotlinSplitterEndpointSpec<MethodInvokingSplitter>.() -> Unit) {

		this.delegate.split(service, methodName) { splitterConfigurer(KotlinSplitterEndpointSpec(it)) }
	}

	/**
	 * Populate the [MethodInvokingSplitter] to evaluate the provided
	 * `method` of the `bean` at runtime.
	 */
	fun split(beanName: String, methodName: String? = null) {
		this.delegate.split(beanName, methodName)
	}

	/**
	 * Populate the [MethodInvokingSplitter] to evaluate the provided
	 * `method` of the `bean` at runtime.
	 * In addition accept options for the integration endpoint using [KotlinSplitterEndpointSpec].
	 */
	fun split(beanName: String, methodName: String?,
			  splitterConfigurer: KotlinSplitterEndpointSpec<MethodInvokingSplitter>.() -> Unit) {

		this.delegate.split(beanName, methodName) { splitterConfigurer(KotlinSplitterEndpointSpec(it)) }
	}

	/**
	 * Populate the [MethodInvokingSplitter] to evaluate the
	 * [MessageProcessor] at runtime
	 * from provided [MessageProcessorSpec].
	 * In addition accept options for the integration endpoint using [KotlinSplitterEndpointSpec].
	 */
	fun split(messageProcessorSpec: MessageProcessorSpec<*>,
			  splitterConfigurer: KotlinSplitterEndpointSpec<MethodInvokingSplitter>.() -> Unit = {}) {

		this.delegate.split(messageProcessorSpec) { splitterConfigurer(KotlinSplitterEndpointSpec(it)) }
	}

	/**
	 * Populate the provided [AbstractMessageSplitter] to the current integration flow position.
	 */
	fun <S : AbstractMessageSplitter> split(splitterMessageHandlerSpec: MessageHandlerSpec<*, S>,
											splitterConfigurer: KotlinSplitterEndpointSpec<S>.() -> Unit = {}) {

		this.delegate.split(splitterMessageHandlerSpec) { splitterConfigurer(KotlinSplitterEndpointSpec(it)) }
	}

	/**
	 * Populate the provided [AbstractMessageSplitter] to the current integration
	 * flow position.
	 */
	fun <S : AbstractMessageSplitter> split(splitter: S,
											splitterConfigurer: KotlinSplitterEndpointSpec<S>.() -> Unit = {}) {

		this.delegate.split(splitter) { splitterConfigurer(KotlinSplitterEndpointSpec(it)) }
	}

	/**
	 * Provide the [HeaderFilter] to the current [IntegrationFlow].
	 */
	fun headerFilter(headersToRemove: String, patternMatch: Boolean = true) {
		this.delegate.headerFilter(headersToRemove, patternMatch)
	}

	/**
	 * Populate the provided [MessageTransformingHandler] for the provided
	 * [HeaderFilter].
	 */
	fun headerFilter(headerFilter: HeaderFilter,
					 endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit) {

		this.delegate.headerFilter(headerFilter, endpointConfigurer)
	}

	/**
	 * Populate the [MessageTransformingHandler] for the [ClaimCheckInTransformer]
	 * with provided [MessageStore].
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun claimCheckIn(messageStore: MessageStore,
					 endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit = {}) {

		this.delegate.claimCheckIn(messageStore, endpointConfigurer)
	}

	/**
	 * Populate the [MessageTransformingHandler] for the [ClaimCheckOutTransformer]
	 * with provided [MessageStore] and `removeMessage` flag.
	 */
	fun claimCheckOut(messageStore: MessageStore, removeMessage: Boolean = false) {
		this.delegate.claimCheckOut(messageStore, removeMessage)
	}

	/**
	 * Populate the [MessageTransformingHandler] for the [ClaimCheckOutTransformer]
	 * with provided [MessageStore] and `removeMessage` flag.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun claimCheckOut(messageStore: MessageStore, removeMessage: Boolean,
					  endpointConfigurer: GenericEndpointSpec<MessageTransformingHandler>.() -> Unit) {

		this.delegate.claimCheckOut(messageStore, removeMessage, endpointConfigurer)
	}

	/**
	 * Populate the
	 * [org.springframework.integration.aggregator.ResequencingMessageHandler] with
	 * provided options from [ResequencerSpec].
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun resequence(resequencer: ResequencerSpec.() -> Unit = {}) {
		this.delegate.resequence(resequencer)
	}

	/**
	 * Populate the [AggregatingMessageHandler] with provided options from [AggregatorSpec].
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun aggregate(aggregator: AggregatorSpec.() -> Unit = {}) {
		this.delegate.aggregate(aggregator)
	}

	/**
	 * Populate the [MethodInvokingRouter] for provided bean and its method
	 * with default options.
	 */
	fun route(beanName: String, method: String? = null) {
		this.delegate.route(beanName, method)
	}

	/**
	 * Populate the [MethodInvokingRouter] for provided bean and its method
	 * with provided options from [KotlinRouterSpec].
	 */
	fun route(beanName: String, method: String?,
			  routerConfigurer: KotlinRouterSpec<Any, MethodInvokingRouter>.() -> Unit) {

		this.delegate.route(beanName, method) { routerConfigurer(KotlinRouterSpec(it)) }
	}

	/**
	 * Populate the [MethodInvokingRouter] for the method
	 * of the provided service and its method with default options.
	 */
	fun route(service: Any, methodName: String? = null) {
		this.delegate.route(service, methodName)
	}

	/**
	 * Populate the [MethodInvokingRouter] for the method
	 * of the provided service and its method with provided options from [KotlinRouterSpec].
	 */
	fun route(service: Any, methodName: String?,
			  routerConfigurer: KotlinRouterSpec<Any, MethodInvokingRouter>.() -> Unit) {

		this.delegate.route(service, methodName) { routerConfigurer(KotlinRouterSpec(it)) }
	}

	/**
	 * Populate the [ExpressionEvaluatingRouter] for provided SpEL expression
	 * with provided options from [KotlinRouterSpec].
	 */
	fun <T> route(expression: String,
				  routerConfigurer: KotlinRouterSpec<T, ExpressionEvaluatingRouter>.() -> Unit = {}) {

		this.delegate.route<T>(expression) { routerConfigurer(KotlinRouterSpec(it)) }
	}

	/**
	 * Populate the [MethodInvokingRouter] for the
	 * [MessageProcessor]
	 * from the provided [MessageProcessorSpec] with default options.
	 */
	fun route(messageProcessorSpec: MessageProcessorSpec<*>,
			  routerConfigurer: KotlinRouterSpec<Any, MethodInvokingRouter>.() -> Unit = {}) {

		this.delegate.route(messageProcessorSpec) { routerConfigurer(KotlinRouterSpec(it)) }
	}

	/**
	 * Populate the [RecipientListRouter] with options from the [KotlinRecipientListRouterSpec].
	 */
	fun routeToRecipients(routerConfigurer: KotlinRecipientListRouterSpec.() -> Unit) {
		this.delegate.routeToRecipients { routerConfigurer(KotlinRecipientListRouterSpec(it)) }
	}

	/**
	 * Populate the [ErrorMessageExceptionTypeRouter] with options from the [KotlinRouterSpec].
	 */
	fun routeByException(
			routerConfigurer: KotlinRouterSpec<Class<out Throwable>, ErrorMessageExceptionTypeRouter>.() -> Unit) {

		this.delegate.routeByException { routerConfigurer(KotlinRouterSpec(it)) }
	}

	/**
	 * Populate the provided [AbstractMessageRouter] implementation to the
	 * current integration flow position.
	 * In addition accept options for the integration endpoint using [GenericEndpointSpec].
	 */
	fun <R : AbstractMessageRouter?> route(router: R, endpointConfigurer: GenericEndpointSpec<R>.() -> Unit = {}) {
		this.delegate.route(router, endpointConfigurer)
	}

	/**
	 * Populate the "artificial"
	 * [org.springframework.integration.gateway.GatewayMessageHandler] for the
	 * provided `requestChannel` to send a request with options from
	 * [GatewayEndpointSpec]. Uses
	 * [org.springframework.integration.gateway.RequestReplyExchanger] Proxy on the
	 * background.
	 */
	fun gateway(requestChannel: String, endpointConfigurer: GatewayEndpointSpec.() -> Unit = {}) {
		this.delegate.gateway(requestChannel, endpointConfigurer)
	}

	/**
	 * Populate the "artificial"
	 * [org.springframework.integration.gateway.GatewayMessageHandler] for the
	 * provided `requestChannel` to send a request with options from
	 * [GatewayEndpointSpec]. Uses
	 * [org.springframework.integration.gateway.RequestReplyExchanger] Proxy on the
	 * background.
	 */
	fun gateway(requestChannel: MessageChannel, endpointConfigurer: GatewayEndpointSpec.() -> Unit = {}) {
		this.delegate.gateway(requestChannel, Consumer(endpointConfigurer))
	}

	/**
	 * Populate the "artificial"
	 * [org.springframework.integration.gateway.GatewayMessageHandler] for the
	 * provided `subflow` with options from [GatewayEndpointSpec].
	 */
	fun gateway(flow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.gateway(IntegrationFlow { flow(KotlinIntegrationFlowDefinition(it)) })
	}

	/**
	 * Populate the "artificial"
	 * [org.springframework.integration.gateway.GatewayMessageHandler] for the
	 * provided `subflow` with options from [GatewayEndpointSpec].
	 */
	fun gateway(endpointConfigurer: GatewayEndpointSpec.() -> Unit,
				flow: KotlinIntegrationFlowDefinition.() -> Unit) {

		this.delegate.gateway(
				IntegrationFlow { flow(KotlinIntegrationFlowDefinition(it)) },
				Consumer(endpointConfigurer))
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the `INFO`
	 * logging level and `org.springframework.integration.handler.LoggingHandler`
	 * as a default logging category.
	 */
	fun log() {
		this.delegate.log()
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for provided [LoggingHandler.Level]
	 * logging level and `org.springframework.integration.handler.LoggingHandler`
	 * as a default logging category.
	 */
	fun log(level: LoggingHandler.Level, category: String? = null) {
		this.delegate.log(level, category)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the provided logging category
	 * and `INFO` logging level.
	 */
	fun log(category: String) {
		this.delegate.log(category)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the provided
	 * [LoggingHandler.Level] logging level, logging category
	 * and SpEL expression for the log message.
	 */
	fun log(level: LoggingHandler.Level, category: String, logExpression: String) {
		this.delegate.log(level, category, logExpression)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the `INFO` logging level,
	 * the `org.springframework.integration.handler.LoggingHandler`
	 * as a default logging category and function for the log message.
	 */
	fun <P> log(function: (Message<P>) -> Any) {
		this.delegate.log(function)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the `INFO` logging level,
	 * the `org.springframework.integration.handler.LoggingHandler`
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request [Message].
	 */
	fun log(logExpression: Expression) {
		this.delegate.log(logExpression)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the provided
	 * [LoggingHandler.Level] logging level,
	 * the `org.springframework.integration.handler.LoggingHandler`
	 * as a default logging category and SpEL expression to evaluate
	 * logger message at runtime against the request [Message].
	 *  When this operator is used in the end of flow, it is treated
	 * as one-way handler without any replies to continue.
	 */
	fun log(level: LoggingHandler.Level, logExpression: Expression) {
		this.delegate.log(level, logExpression)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the `INFO`
	 * [LoggingHandler.Level] logging level,
	 * the provided logging category and SpEL expression to evaluate
	 * logger message at runtime against the request [Message].
	 */
	fun log(category: String, logExpression: Expression) {
		this.delegate.log(category, logExpression)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the provided
	 * [LoggingHandler.Level] logging level,
	 * the `org.springframework.integration.handler.LoggingHandler`
	 * as a default logging category and function for the log message.
	 */
	fun <P> log(level: LoggingHandler.Level, function: (Message<P>) -> Any) {
		this.delegate.log(level, function)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the provided
	 * [LoggingHandler.Level] logging level,
	 * the provided logging category and function for the log message.
	 */
	fun <P> log(category: String, function: (Message<P>) -> Any) {
		this.delegate.log(category, function)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the provided
	 * [LoggingHandler.Level] logging level, logging category
	 * and function for the log message.
	 */
	fun <P> log(level: LoggingHandler.Level, category: String, function: (Message<P>) -> Any) {
		this.delegate.log(level, category, function)
	}

	/**
	 * Populate a [WireTap] for the current channel
	 * with the [LoggingHandler] subscriber for the provided
	 * [LoggingHandler.Level] logging level, logging category
	 * and SpEL expression for the log message.
	 */
	fun log(level: LoggingHandler.Level, category: String, logExpression: Expression) {
		this.delegate.log(level, category, logExpression)
	}

	/**
	 * Populate a [ScatterGatherHandler] to the current integration flow position
	 * based on the provided [MessageChannel] for scattering function
	 * and [AggregatorSpec] for gathering function.
	 */
	fun scatterGather(scatterChannel: MessageChannel, gatherer: AggregatorSpec.() -> Unit = {}) {
		this.delegate.scatterGather(scatterChannel, Consumer(gatherer))
	}

	/**
	 * Populate a [ScatterGatherHandler] to the current integration flow position
	 * based on the provided [MessageChannel] for scattering function
	 * and [AggregatorSpec] for gathering function.
	 */
	fun scatterGather(scatterChannel: MessageChannel, gatherer: AggregatorSpec.() -> Unit,
					  scatterGather: ScatterGatherSpec.() -> Unit) {

		this.delegate.scatterGather(scatterChannel, Consumer(gatherer), Consumer(scatterGather))
	}

	/**
	 * Populate a [ScatterGatherHandler] to the current integration flow position
	 * based on the provided [KotlinRecipientListRouterSpec] for scattering function
	 * and default [AggregatorSpec] for gathering function.
	 */
	fun scatterGather(scatterer: KotlinRecipientListRouterSpec.() -> Unit) {
		this.delegate.scatterGather(Consumer { scatterer(KotlinRecipientListRouterSpec(it)) })
	}

	/**
	 * Populate a [ScatterGatherHandler] to the current integration flow position
	 * based on the provided [KotlinRecipientListRouterSpec] for scattering function
	 * and [AggregatorSpec] for gathering function.
	 */
	fun scatterGather(scatterer: KotlinRecipientListRouterSpec.() -> Unit, gatherer: AggregatorSpec.() -> Unit) {
		this.delegate.scatterGather(Consumer { scatterer(KotlinRecipientListRouterSpec(it)) },
				Consumer { gatherer(it) })
	}

	/**
	 * Populate a [ScatterGatherHandler] to the current integration flow position
	 * based on the provided [KotlinRecipientListRouterSpec] for scattering function
	 * and [AggregatorSpec] for gathering function.
	 */
	fun scatterGather(scatterer: KotlinRecipientListRouterSpec.() -> Unit, gatherer: AggregatorSpec.() -> Unit,
					  scatterGather: ScatterGatherSpec.() -> Unit) {

		this.delegate.scatterGather(Consumer { scatterer(KotlinRecipientListRouterSpec(it)) },
				Consumer { gatherer(it) }, Consumer { scatterGather(it) })
	}

	/**
	 * Populate a [org.springframework.integration.aggregator.BarrierMessageHandler]
	 * instance for provided timeout and options from [BarrierSpec] and endpoint
	 * options from [GenericEndpointSpec].
	 */
	fun barrier(timeout: Long, barrierConfigurer: BarrierSpec.() -> Unit = {}) {
		this.delegate.barrier(timeout, barrierConfigurer)
	}

	/**
	 * Populate a [ServiceActivatingHandler] instance to perform [MessageTriggerAction]
	 * and endpoint options from [GenericEndpointSpec].
	 */
	fun trigger(triggerActionId: String,
				endpointConfigurer: GenericEndpointSpec<ServiceActivatingHandler>.() -> Unit = {}) {

		this.delegate.trigger(triggerActionId, endpointConfigurer)
	}

	/**
	 * Populate a [ServiceActivatingHandler] instance to perform [MessageTriggerAction]
	 * and endpoint options from [GenericEndpointSpec].
	 */
	fun trigger(triggerAction: MessageTriggerAction,
				endpointConfigurer: GenericEndpointSpec<ServiceActivatingHandler>.() -> Unit = {}) {

		this.delegate.trigger(triggerAction, Consumer(endpointConfigurer))
	}

	/**
	 * Populate a [FluxMessageChannel] to start a reactive processing for upstream data,
	 * wrap it to a [Flux], apply provided function via [Flux.transform]
	 * and emit the result to one more [FluxMessageChannel], subscribed in the downstream flow.
	 */
	fun <I, O> fluxTransform(fluxFunction: (Flux<Message<I>>) -> Publisher<O>) {
		this.delegate.fluxTransform(fluxFunction)
	}

}
