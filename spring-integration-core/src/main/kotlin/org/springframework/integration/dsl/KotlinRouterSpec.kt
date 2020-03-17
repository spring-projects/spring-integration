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

	fun noChannelKeyFallback() {
		this.delegate.noChannelKeyFallback()
	}

	fun channelMapping(key: K, channelName: String) {
		this.delegate.channelMapping(key, channelName)
	}

	fun channelMapping(key: K, channel: MessageChannel) {
		this.delegate.channelMapping(key, channel)
	}

	fun subFlowMapping(key: K, subFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.subFlowMapping(key) { subFlow(KotlinIntegrationFlowDefinition(it)) }
	}

}
