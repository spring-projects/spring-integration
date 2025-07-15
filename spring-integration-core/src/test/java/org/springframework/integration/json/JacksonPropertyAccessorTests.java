/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JacksonPropertyAccessor}.
 *
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 * @see JacksonIndexAccessorTests
 */
public class JacksonPropertyAccessorTests extends AbstractJacksonAccessorTests {

	@BeforeEach
	void registerJsonPropertyAccessor() {
		context.addPropertyAccessor(new JacksonPropertyAccessor());
	}

	/**
	 * Tests which index directly into a Jackson {@link ArrayNode}, which is not supported
	 * by {@link JacksonPropertyAccessor}.
	 */
	@Nested
	class ArrayNodeTests {

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndex() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			assertIndexingNotSupported(arrayNode, "[1]");
		}

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndexForNullValue() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, null, 5]");
			assertIndexingNotSupported(arrayNode, "[1]");
		}

		@Test
		void indexDirectlyIntoArrayNodeWithNegativeIntegerIndex() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			assertIndexingNotSupported(arrayNode, "[-1]");
		}

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndexOutOfBounds() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			assertIndexingNotSupported(arrayNode, "[9999]");
		}

		/**
		 * @see JsonNodeTests#nestedArrayLookupWithStringIndexAndThenIntegerIndex()
		 */
		@Test
		void nestedArrayLookupWithIntegerIndexAndThenIntegerIndex() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[[3], [4, 5], []]");
			assertIndexingNotSupported(arrayNode, "[1][1]");
		}

		private void assertIndexingNotSupported(ArrayNode arrayNode, String expression) {
			assertThatExceptionOfType(SpelEvaluationException.class)
					.isThrownBy(() -> parser.parseExpression(expression).getValue(context, arrayNode))
					.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE));
		}

	}

}
