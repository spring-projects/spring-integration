/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
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
