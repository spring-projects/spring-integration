/*
 * Copyright 2020-2023 the original author or authors.
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

import org.springframework.integration.splitter.AbstractMessageSplitter
import org.springframework.messaging.MessageChannel

/**
 * An  [SplitterEndpointSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [SplitterEndpointSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
@Deprecated("since 6.2", ReplaceWith("KotlinSplitterSpec"))
@Suppress("REMOVAL", "DEPRECATION")
class KotlinSplitterEndpointSpec<H : AbstractMessageSplitter>(override val delegate: SplitterEndpointSpec<H>)
	: KotlinConsumerEndpointSpec<SplitterEndpointSpec<H>, H>(delegate) {

	fun applySequence(applySequence: Boolean) {
		this.delegate.applySequence(applySequence)
	}

	fun delimiters(delimiters: String) {
		this.delegate.delimiters(delimiters)
	}

	fun discardChannel(discardChannel: MessageChannel) {
		this.delegate.discardChannel(discardChannel)
	}

	fun discardChannel(discardChannelName: String) {
		this.delegate.discardChannel(discardChannelName)
	}

	fun discardFlow(discardFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		discardFlow {definition -> discardFlow(KotlinIntegrationFlowDefinition(definition)) }
	}

	fun discardFlow(discardFlow: IntegrationFlow) {
		this.delegate.discardFlow(discardFlow)
	}

}
