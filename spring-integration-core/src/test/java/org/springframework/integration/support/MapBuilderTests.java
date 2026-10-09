/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.support;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 *
 * @since 7.1
 */
class MapBuilderTests {

	@Test
	void testPutMultipleEntries() {
		TestMapBuilder builder = new TestMapBuilder();
		Map<String, Object> map = builder
				.put("key1", "value1")
				.put("key2", "value2")
				.put("key3", "value3")
				.get();

		assertThat(map)
				.containsEntry("key1", "value1")
				.containsEntry("key2", "value2")
				.containsEntry("key3", "value3");
	}

	@Test
	void testPutNullValue() {
		TestMapBuilder builder = new TestMapBuilder();
		Map<String, Object> map = builder
				.put("key1", "value1")
				.put("key2", null)
				.get();

		assertThat(map)
				.containsEntry("key1", "value1")
				.containsEntry("key2", null);
	}

	static class TestMapBuilder extends MapBuilder<TestMapBuilder, String, Object> {

	}

}
