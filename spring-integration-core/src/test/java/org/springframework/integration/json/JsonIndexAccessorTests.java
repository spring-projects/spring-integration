/*
 * Copyright 2013-2024 the original author or authors.
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.integration.json.JsonPropertyAccessor.ArrayNodeAsList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonIndexAccessor} combined with {@link JsonPropertyAccessor}.
 *
 * @author Sam Brannen
 * @since 6.4
 * @see JsonPropertyAccessorTests
 */
class JsonIndexAccessorTests extends AbstractJsonAccessorTests {

	@BeforeEach
	void registerJsonAccessors() {
		context.addIndexAccessor(new JsonIndexAccessor());
		// We also register a JsonPropertyAccessor to ensure that the JsonIndexAccessor
		// does not interfere with the feature set of the JsonPropertyAccessor.
		context.addPropertyAccessor(new JsonPropertyAccessor());
	}

	/**
	 * Tests which index directly into a Jackson {@link ArrayNode}, which is only supported
	 * by {@link JsonIndexAccessor}.
	 */
	@Nested
	class ArrayNodeTests {

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndex() throws Exception {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[1]", Integer.class);
			assertThat(actual).isEqualTo(4);
		}

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndexForNullValue() throws Exception {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, null, 5]");
			Integer actual = evaluate(arrayNode, "[1]", Integer.class);
			assertThat(actual).isNull();
		}

		@Test
		void indexDirectlyIntoArrayNodeWithNegativeIntegerIndex() throws Exception {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[-1]", Integer.class);
			// JsonIndexAccessor allows one to index into a JSON array via a negative index.
			assertThat(actual).isEqualTo(5);
		}

		@Test
		void indexDirectlyIntoArrayNodeWithNegativeIntegerIndexGreaterThanArrayLength() throws Exception {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[-99]", Integer.class);
			// Although JsonIndexAccessor allows one to index into a JSON array via a negative
			// index, if the result of (array.length - index) is still negative, Jackson's
			// ArrayNode.get() method returns null instead of throwing an IndexOutOfBoundsException.
			assertThat(actual).isNull();
		}

		@Test
		void indexDirectlyIntoArrayNodeWithIntegerIndexOutOfBounds() throws Exception {
			ArrayNode arrayNode = (ArrayNode) mapper.readTree("[3, 4, 5]");
			Integer actual = evaluate(arrayNode, "[9999]", Integer.class);
			// Jackson's ArrayNode.get() method always returns null instead of throwing an IndexOutOfBoundsException.
			assertThat(actual).isNull();
		}

		/**
		 * @see AbstractJsonAccessorTests.JsonNodeTests#nestedArrayLookupWithStringIndexAndThenIntegerIndex()
		 */
		@Test
		@SuppressWarnings("unchecked")
		void nestedArrayLookupsWithIntegerIndexes() throws Exception {
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
