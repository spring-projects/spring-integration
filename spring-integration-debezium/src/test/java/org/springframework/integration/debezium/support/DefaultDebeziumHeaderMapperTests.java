/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.integration.debezium.support;

import java.util.List;

import io.debezium.engine.Header;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class DefaultDebeziumHeaderMapperTests {

	final DefaultDebeziumHeaderMapper mapper = new DefaultDebeziumHeaderMapper();

	final List<Header<Object>> debeziumHeaders = List.of(
			new TestHeader<>("NonStandard1", "NonStandard1_Value"),
			new TestHeader<>("NonStandard2", "NonStandard2_Value"));

	@Test
	public void fromHeaderNotSupported() {
		assertThatThrownBy(() -> mapper.fromHeaders(null, null))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("The 'fromHeaders' is not supported!");
	}

	@Test
	public void defaultHeaders() {
		assertThat(mapper.toHeaders(debeziumHeaders)).hasSize(4)
				.containsKeys("NonStandard1", "NonStandard2", "id", "timestamp");
	}

	@Test
	public void disableDebeziumHeaders() {
		mapper.setHeaderNamesToMap("");
		assertThat(mapper.toHeaders(debeziumHeaders)).hasSize(2)
				.containsKeys("id", "timestamp");
	}

	@Test
	public void exactCustomHeaderName() {
		mapper.setHeaderNamesToMap("NonStandard1");

		assertThat(mapper.toHeaders(debeziumHeaders)).hasSize(3)
				.containsKeys("NonStandard1", "id", "timestamp");
	}

	@Test
	public void headerNamePattern() {
		mapper.setHeaderNamesToMap("NonStandard*");

		assertThat(mapper.toHeaders(debeziumHeaders)).hasSize(4)
				.containsKeys("NonStandard1", "NonStandard2", "id", "timestamp");

	}

	public record TestHeader<T>(String key, T value) implements Header<T> {

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public T getValue() {
			return value;
		}

	}

}
