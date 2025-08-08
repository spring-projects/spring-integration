/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.dsl

import org.springframework.integration.filter.MessageFilter
import org.springframework.messaging.MessageChannel

/**
 * An  [FilterEndpointSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [FilterEndpointSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
class KotlinFilterEndpointSpec(override val delegate: FilterEndpointSpec)
	: KotlinConsumerEndpointSpec<FilterEndpointSpec, MessageFilter>(delegate) {

	fun throwExceptionOnRejection(throwExceptionOnRejection: Boolean) {
		this.delegate.throwExceptionOnRejection(throwExceptionOnRejection)
	}

	fun discardChannel(discardChannel: MessageChannel) {
		this.delegate.discardChannel(discardChannel)
	}

	fun discardChannel(discardChannelName: String) {
		this.delegate.discardChannel(discardChannelName)
	}

	fun discardWithinAdvice(discardWithinAdvice: Boolean) {
		this.delegate.discardWithinAdvice(discardWithinAdvice)
	}

	fun discardFlow(subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		discardFlow {definition -> subFlow(KotlinIntegrationFlowDefinition(definition)) }
	}

	fun discardFlow(subFlow: IntegrationFlow) {
		this.delegate.discardFlow(subFlow)
	}

}
