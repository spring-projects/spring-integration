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

import java.util.ArrayList;
import java.util.List;

import io.debezium.engine.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Christian Tzolov
 * @since 6.2
 */
public class DefaultDebeziumHeaderMapperTests {

	DefaultDebeziumHeaderMapper mapper;

	List<Header<Object>> debeziumHeaders;

	@BeforeEach
	public void beforeEach() {
		mapper = new DefaultDebeziumHeaderMapper();

		debeziumHeaders = new ArrayList<>();
		debeziumHeaders.add(new TestHeader<Object>("NonStandard1", "NonStandard1_Value"));
		debeziumHeaders.add(new TestHeader<Object>("NonStandard2", "NonStandard2_Value"));
	}

	@Test
	public void fromHeaderNotSupported() {
		assertThatThrownBy(() -> mapper.fromHeaders(null, null))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("The 'fromHeaders' is not supported!");
	}

	@Test
	public void defaultHeaders() {
		assertThat(mapper.toHeaders(debeziumHeaders)).hasSize(2).containsKeys("id",
				"timestamp");
	}

	@Test
	public void exactCustomHeaderName() {
		mapper.setAllowedHeaderNames("NonStandard1");

		assertThat(mapper.toHeaders(debeziumHeaders)).hasSize(3).containsKeys("NonStandard1", "id",
				"timestamp");
	}

	@Test
	public void headerNamePattern() {

		mapper.setAllowedHeaderNames("NonStandard*");

		assertThat(mapper.toHeaders(debeziumHeaders)).hasSize(4).containsKeys("NonStandard1", "NonStandard2", "id",
				"timestamp");

	}

	public static class TestHeader<T> implements Header<T> {

		private final String key;
		private final T value;

		public TestHeader(String key, T value) {
			this.key = key;
			this.value = value;
		}

		public TestHeader(Header<T> header) {
			this.key = header.getKey();
			this.value = header.getValue();
		}

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
