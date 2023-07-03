/*
 * Copyright 2023 the original author or authors.
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

/**
 * A [SplitterSpec] wrapped for Kotlin DSL.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
class KotlinSplitterSpec : SplitterSpec() {

	/**
	 * Provide a Kotlin function as a direct delegate for
	 * [org.springframework.integration.splitter.MethodInvokingSplitter].
	 * @param function the function instance to use.
	 * @param <P> the input type.
	 */
	inline fun <reified P> function(crossinline function: (P) -> Any) {
		expectedType(P::class.java)
		function<P> { function(it) }
	}

	fun discardFlow(discardFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		discardFlow {definition -> discardFlow(KotlinIntegrationFlowDefinition(definition)) }
	}

}
