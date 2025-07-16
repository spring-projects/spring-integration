/*
 * Copyright 2013-present the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import org.springframework.integration.json.JacksonPropertyAccessor.ArrayNodeAsList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JacksonIndexAccessor} combined with {@link JacksonPropertyAccessor}.
 *
 * @author Sam Brannen
 * @author Jooyoung Pyoung
 *
 * @since 6.4
 *
 * @see JacksonPropertyAccessorTests
 */
class JacksonIndexAccessorTests extends AbstractJacksonAccessorTests {

	@BeforeEach
	void registerJsonAccessors() {
		context.addIndexAccessor(new JacksonIndexAccessor());
		// We also register a JacksonPropertyAccessor to ensure that the JacksonIndexAccessor
		// does not interfere with the feature set of the JacksonPropertyAccessor.
		context.addPropertyAccessor(new JacksonPropertyAccessor());
	}

	/**
	 * Tests which index directly into a Jackson {@link ArrayNode}, which is only supported
	 * by {@link JacksonPropertyAccessor}.
	 */
	@Nested
	class ArrayNodeTests {

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndex() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[1]", Integer.class);
			assertThat(actual).isEqualTo(4);
		}

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndexForNullValue() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, null, 5]");
			Integer actual = evaluate(arrayNode, "[1]", Integer.class);
			assertThat(actual).isNull();
		}

		@Test
		void indexDirectlyIntoArrayNodeWithNegativeIntegerIndex() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[-1]", Integer.class);
			// JacksonIndexAccessor allows one to index into a JSON array via a negative index.
			assertThat(actual).isEqualTo(5);
		}

		@Test
		void indexDirectlyIntoArrayNodeWithNegativeIntegerIndexGreaterThanArrayLength() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[-99]", Integer.class);
			// Although JacksonIndexAccessor allows one to index into a JSON array via a negative
			// index, if the result of (array.length - index) is still negative, Jackson's
			// ArrayNode.get() method returns null instead of throwing an IndexOutOfBoundsException.
			assertThat(actual).isNull();
		}

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndexOutOfBounds() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[9999]", Integer.class);
			// Jackson's ArrayNode.get() method always returns null instead of throwing an IndexOutOfBoundsException.
			assertThat(actual).isNull();
		}

		/**
		 * @see JsonNodeTests#nestedArrayLookupWithStringIndexAndThenIntegerIndex()
		 */
		@Test
		@SuppressWarnings("unchecked")
		void nestedArrayLookupsWithIntegerIndexes() {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[[3], [4, 5], []]");

			List<Integer> list = evaluate(arrayNode, "[0]", List.class);
			assertThat(list).isInstanceOf(ArrayNodeAsList.class).containsExactly(3);
			list = evaluate(arrayNode, "[2]", List.class);
			assertThat(list).isInstanceOf(ArrayNodeAsList.class).isEmpty();

			Integer number = evaluate(arrayNode, "[0][0]", Integer.class);
			assertThat(number).isEqualTo(3);
			number = evaluate(arrayNode, "[1][1]", Integer.class);
			assertThat(number).isEqualTo(5);
		}

	}

}
