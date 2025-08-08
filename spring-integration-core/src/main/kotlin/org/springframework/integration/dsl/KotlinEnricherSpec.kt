/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.dsl

import org.springframework.integration.transformer.ContentEnricher
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel

/**
 * An  [EnricherSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [EnricherSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
class KotlinEnricherSpec(override val delegate: EnricherSpec)
	: KotlinConsumerEndpointSpec<EnricherSpec, ContentEnricher>(delegate) {

	fun requestChannel(requestChannel: MessageChannel) {
		this.delegate.requestChannel(requestChannel)
	}

	fun requestChannel(requestChannel: String) {
		this.delegate.requestChannel(requestChannel)
	}

	fun replyChannel(replyChannel: MessageChannel) {
		this.delegate.replyChannel(replyChannel)
	}

	fun replyChannel(replyChannel: String) {
		this.delegate.replyChannel(replyChannel)
	}

	fun errorChannel(errorChannel: MessageChannel) {
		this.delegate.errorChannel(errorChannel)
	}

	fun errorChannel(errorChannel: String) {
		this.delegate.errorChannel(errorChannel)
	}

	fun requestTimeout(requestTimeout: Long) {
		this.delegate.requestTimeout(requestTimeout)
	}

	fun replyTimeout(replyTimeout: Long) {
		this.delegate.replyTimeout(replyTimeout)
	}

	fun requestPayloadExpression(requestPayloadExpression: String) {
		this.delegate.requestPayloadExpression(requestPayloadExpression)
	}

	fun <P> requestPayload(function: (Message<P>) -> Any) {
		this.delegate.requestPayload(function)
	}

	fun requestSubFlow(subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		requestSubFlow {definition -> subFlow(KotlinIntegrationFlowDefinition(definition)) }
	}

	fun requestSubFlow(subFlow: IntegrationFlow) {
		this.delegate.requestSubFlow(subFlow)
	}

	fun shouldClonePayload(shouldClonePayload: Boolean) {
		this.delegate.shouldClonePayload(shouldClonePayload)
	}

	fun property(key: String, value: Any) {
		this.delegate.property(key, value)
	}

	fun propertyExpression(key: String, expression: String) {
		this.delegate.propertyExpression(key, expression)
	}

	fun <P> propertyFunction(key: String, function: (Message<P>) -> Any) {
		this.delegate.propertyFunction(key, function)
	}

	fun header(name: String, value: Any, overwrite: Boolean?) {
		this.delegate.header(name, value, overwrite)
	}

	fun headerExpression(name: String, expression: String, overwrite: Boolean?) {
		this.delegate.header(name, expression, overwrite)
	}

	fun <P> headerFunction(name: String, function: (Message<P>) -> Any, overwrite: Boolean?) {
		this.delegate.header(name, function, overwrite)
	}

	fun header(headerName: String, headerValueMessageProcessor: HeaderValueMessageProcessor<Any>) {
		this.delegate.header(headerName, headerValueMessageProcessor)
	}

}
