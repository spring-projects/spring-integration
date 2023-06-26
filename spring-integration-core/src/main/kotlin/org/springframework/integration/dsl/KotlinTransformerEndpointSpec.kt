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

import org.springframework.integration.transformer.MessageTransformingHandler

/**
 * A [TransformerEndpointSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [TransformerEndpointSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
class KotlinTransformerEndpointSpec : TransformerEndpointSpec() {

	/**
	 * Provide a Kotlin function as a direct delegate for [MessageTransformingHandler].
	 * @param function the function instance to use.
	 * @param <P> the input type.
	 */
	inline fun <reified P> transformer(crossinline function: (P) -> Any) {
		expectedType(P::class.java)
		transformer<P, Any> { function(it) }
	}

}
