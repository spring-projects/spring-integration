/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.dsl

import org.reactivestreams.Publisher
import org.springframework.integration.core.MessageSource
import org.springframework.integration.endpoint.MessageProducerSupport
import org.springframework.integration.gateway.MessagingGatewaySupport
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel

private fun buildIntegrationFlow(flowBuilder: IntegrationFlowBuilder,
								 flow: (KotlinIntegrationFlowDefinition) -> Unit) =
	KotlinIntegrationFlowDefinition(flowBuilder).apply(flow).delegate.get()

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow] lambdas.
 *
 * @author Artem Bilan
 */
fun integrationFlow(flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		IntegrationFlow {
			KotlinIntegrationFlowDefinition(it).flow()
		}

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(Class<?>, Consumer<GatewayProxySpec>)` factory method.
 *
 * @author Artem Bilan
 */
inline fun <reified T> integrationFlow(
		crossinline gateway: GatewayProxySpec.() -> Unit = {},
		flow: KotlinIntegrationFlowDefinition.() -> Unit): IntegrationFlow {

	val flowBuilder = IntegrationFlow.from(T::class.java) { gateway(it) }
	KotlinIntegrationFlowDefinition(flowBuilder).flow()
	return flowBuilder.get()
}

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(String, Boolean)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(channelName: String, fixedSubscriber: Boolean = false,
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(channelName, fixedSubscriber), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(MessageChannel)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(channel: MessageChannel, flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(channel), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from]  -
 * `IntegrationFlow.from(MessageSource<*>, Consumer<SourcePollingChannelAdapterSpec>)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(messageSource: MessageSource<*>,
					options: SourcePollingChannelAdapterSpec.() -> Unit = {},
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(messageSource) { options(it) }, flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from]  -
 * `IntegrationFlow.from(MessageSourceSpec<*>, Consumer<SourcePollingChannelAdapterSpec>)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(messageSource: MessageSourceSpec<*, out MessageSource<*>>,
					options: SourcePollingChannelAdapterSpec.() -> Unit = {},
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(messageSource, options), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(Supplier<*>, Consumer<SourcePollingChannelAdapterSpec>)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(source: () -> Any,
					options: SourcePollingChannelAdapterSpec.() -> Unit = {},
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.fromSupplier(source, options), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(Publisher<out Message<*>>)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(publisher: Publisher<out Message<*>>,
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(publisher), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(MessagingGatewaySupport)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(gateway: MessagingGatewaySupport,
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(gateway), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(MessagingGatewaySpec<*, *>)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(gatewaySpec: MessagingGatewaySpec<*, *>,
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(gatewaySpec), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(MessageProducerSupport)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(producer: MessageProducerSupport,
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(producer), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(MessageProducerSpec<*, *>)` factory method.
 *
 * @author Artem Bilan
 */
fun integrationFlow(producerSpec: MessageProducerSpec<*, *>,
					flow: KotlinIntegrationFlowDefinition.() -> Unit) =
		buildIntegrationFlow(IntegrationFlow.from(producerSpec), flow)

/**
 * Functional [IntegrationFlow] definition in Kotlin DSL for [IntegrationFlow.from] -
 * `IntegrationFlow.from(IntegrationFlow)` factory method.
 *
 * @author Artem Bilan
 *
 * @since 5.5.8
 */
fun integrationFlow(sourceFlow: IntegrationFlow, flow: KotlinIntegrationFlowDefinition.() -> Unit) =
	buildIntegrationFlow(IntegrationFlow.from(sourceFlow), flow)

