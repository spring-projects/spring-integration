/*
 * Copyright 2013-present the original author or authors.
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
 * @author Eric Bottard
 * @author Artem Bilan
 * @author Paul Martin
 * @author Pierre Lakreb
 * @author Sam Brannen
 * @author Jooyoung Pyoung
 *
 * @since 3.0
 *
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
