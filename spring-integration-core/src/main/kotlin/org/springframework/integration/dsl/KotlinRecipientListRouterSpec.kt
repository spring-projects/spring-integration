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

import org.springframework.expression.Expression
import org.springframework.integration.core.GenericSelector
import org.springframework.integration.core.MessageSelector
import org.springframework.integration.router.RecipientListRouter
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel

/**
 * A  [RecipientListRouterSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [RecipientListRouterSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
class KotlinRecipientListRouterSpec(override val delegate: RecipientListRouterSpec)
	: AbstractKotlinRouterSpec<RecipientListRouterSpec, RecipientListRouter>(delegate) {

	fun recipient(channelName: String) {
		this.delegate.recipient(channelName)
	}

	fun recipient(channelName: String, expression: String) {
		this.delegate.recipient(channelName, expression)
	}

	fun recipient(channelName: String, expression: Expression) {
		this.delegate.recipient(channelName, expression)
	}

	inline fun <reified P> recipient(channelName: String, crossinline selector: (P) -> Boolean) {
		if (Message::class.java.isAssignableFrom(P::class.java))
			this.delegate.recipientMessageSelector(channelName) { selector(it as P) }
		else
			this.delegate.recipient<P>(channelName) { selector(it) }
	}

	fun recipient(channel: MessageChannel) {
		this.delegate.recipient(channel)
	}

	fun recipient(channel: MessageChannel, expression: String) {
		this.delegate.recipient(channel, expression)
	}

	fun recipient(channel: MessageChannel, expression: Expression) {
		this.delegate.recipient(channel, expression)
	}

	inline fun <reified P> recipient(channel: MessageChannel, crossinline selector: (P) -> Boolean) {
		if (Message::class.java.isAssignableFrom(P::class.java))
			this.delegate.recipientMessageSelector(channel, MessageSelector { selector(it as P) })
		else
			this.delegate.recipient<P>(channel, GenericSelector { selector(it) })
	}

	inline fun <reified P> recipientFlow(crossinline selector: (P) -> Boolean,
										 crossinline subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {

		if (Message::class.java.isAssignableFrom(P::class.java))
			this.delegate.recipientMessageSelectorFlow({ selector(it as P) })
			{ subFlow(KotlinIntegrationFlowDefinition(it)) }
		else
			this.delegate.recipientFlow<P>({ selector(it) }) { subFlow(KotlinIntegrationFlowDefinition(it)) }

	}

	fun recipientFlow(subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.recipientFlow { subFlow(KotlinIntegrationFlowDefinition(it)) }
	}

	fun recipientFlow(expression: String, subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.recipientFlow(expression) { subFlow(KotlinIntegrationFlowDefinition(it)) }
	}

	fun recipientFlow(expression: Expression, subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.recipientFlow(expression) { subFlow(KotlinIntegrationFlowDefinition(it)) }
	}

}
