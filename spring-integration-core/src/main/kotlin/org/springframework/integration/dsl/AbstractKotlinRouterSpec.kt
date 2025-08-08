/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.dsl

import org.springframework.integration.router.AbstractMessageRouter
import org.springframework.messaging.MessageChannel

/**
 * An  [AbstractRouterSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [AbstractRouterSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
abstract class AbstractKotlinRouterSpec<S : AbstractRouterSpec<S, R>, R : AbstractMessageRouter>(override val delegate: S)
	: KotlinConsumerEndpointSpec<S, R>(delegate) {

	fun ignoreSendFailures(ignoreSendFailures: Boolean) {
		this.delegate.ignoreSendFailures(ignoreSendFailures)
	}

	fun applySequence(applySequence: Boolean) {
		this.delegate.applySequence(applySequence)
	}

	fun defaultOutputChannel(channelName: String) {
		this.delegate.defaultOutputChannel(channelName)
	}

	fun defaultOutputChannel(channel: MessageChannel) {
		this.delegate.defaultOutputChannel(channel)
	}

	fun defaultSubFlowMapping(subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		defaultSubFlowMapping {definition -> subFlow(KotlinIntegrationFlowDefinition(definition)) }
	}

	fun defaultSubFlowMapping(subFlow: IntegrationFlow) {
		this.delegate.defaultSubFlowMapping(subFlow)
	}

	fun defaultOutputToParentFlow() {
		this.delegate.defaultOutputToParentFlow()
	}

}
