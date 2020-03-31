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
class KotlinEnricherSpec(val delegate: EnricherSpec)
	: ConsumerEndpointSpec<EnricherSpec, ContentEnricher>(delegate.handler) {

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
		this.delegate.requestSubFlow { subFlow(KotlinIntegrationFlowDefinition(it)) }
	}

	fun shouldClonePayload(shouldClonePayload: Boolean) {
		this.delegate.shouldClonePayload(shouldClonePayload)
	}

	fun <V> property(key: String, value: V) {
		this.delegate.property(key, value)
	}

	fun propertyExpression(key: String, expression: String) {
		this.delegate.propertyExpression(key, expression)
	}

	fun <P> propertyFunction(key: String, function: (Message<P>) -> Any) {
		this.delegate.propertyFunction(key, function)
	}

	fun <V> header(name: String, value: V, overwrite: Boolean?) {
		this.delegate.header(name, value, overwrite)
	}

	fun headerExpression(name: String, expression: String, overwrite: Boolean?) {
		this.delegate.header(name, expression, overwrite)
	}

	fun <P> headerFunction(name: String, function: (Message<P>) -> Any, overwrite: Boolean?) {
		this.delegate.header(name, function, overwrite)
	}

	fun <V> header(headerName: String, headerValueMessageProcessor: HeaderValueMessageProcessor<V>) {
		this.delegate.header(headerName, headerValueMessageProcessor)
	}

}
