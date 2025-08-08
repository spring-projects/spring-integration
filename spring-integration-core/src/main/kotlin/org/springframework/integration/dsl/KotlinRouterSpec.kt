/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.dsl

import org.springframework.integration.router.AbstractMappingMessageRouter
import org.springframework.messaging.MessageChannel

/**
 * A  [RouterSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [RouterSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
class KotlinRouterSpec<K, R : AbstractMappingMessageRouter>(override val delegate: RouterSpec<K, R>)
	: AbstractKotlinRouterSpec<RouterSpec<K, R>, R>(delegate) {

	fun resolutionRequired(resolutionRequired: Boolean) {
		this.delegate.resolutionRequired(resolutionRequired)
	}

	fun dynamicChannelLimit(dynamicChannelLimit: Int) {
		this.delegate.dynamicChannelLimit(dynamicChannelLimit)
	}

	fun prefix(prefix: String) {
		this.delegate.prefix(prefix)
	}

	fun suffix(suffix: String) {
		this.delegate.suffix(suffix)
	}

	fun channelKeyFallback(channelKeyFallback: Boolean) {
		this.delegate.channelKeyFallback(channelKeyFallback)
	}

	fun channelMapping(key: K & Any, channelName: String) {
		this.delegate.channelMapping(key, channelName)
	}

	fun channelMapping(key: K & Any, channel: MessageChannel) {
		this.delegate.channelMapping(key, channel)
	}

	fun subFlowMapping(key: K & Any, subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		subFlowMapping(key) { definition -> subFlow(KotlinIntegrationFlowDefinition(definition)) }
	}

	fun subFlowMapping(key: K & Any, subFlow: IntegrationFlow) {
		this.delegate.subFlowMapping(key, subFlow)
	}

}
