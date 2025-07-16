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

package org.springframework.integration.support.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.joda.JodaModule;
import tools.jackson.module.kotlin.KotlinModule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Jooyoung Pyoung
 *
 * @since 7.0
 */
class JacksonJsonObjectMapperTests {

	private static final JacksonModule JODA_MODULE = new JodaModule();

	private static final JacksonModule KOTLIN_MODULE = new KotlinModule.Builder().build();

	private JacksonJsonObjectMapper mapper;

	@BeforeEach
	void setUp() {
		mapper = new JacksonJsonObjectMapper();
	}

	@Test
	void compareAutoDiscoveryVsManualModules() {
		ObjectMapper manualMapper = JsonMapper.builder()
				.addModules(collectWellKnownModulesIfAvailable())
				.build();

		Set<String> collectedModuleNames = getModuleNames(collectWellKnownModulesIfAvailable());

		Set<String> autoModuleNames = getModuleNames(mapper.getObjectMapper().getRegisteredModules());
		assertThat(autoModuleNames).isEqualTo(collectedModuleNames);

		Set<String> manualModuleNames = getModuleNames(manualMapper.getRegisteredModules());
		assertThat(manualModuleNames).isEqualTo(collectedModuleNames);
	}

	@Test
	void testToJsonNodeWithVariousInputTypes() throws IOException {
		String jsonString = "{\"name\":\"test\",\"value\":123}";
		JsonNode nodeFromString = mapper.toJsonNode(jsonString);
		assertThat(nodeFromString.get("name").asString()).isEqualTo("test");
		assertThat(nodeFromString.get("value").asInt()).isEqualTo(123);

		byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
		JsonNode nodeFromBytes = mapper.toJsonNode(jsonBytes);
		assertThat(nodeFromBytes).isEqualTo(nodeFromString);

		try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
			JsonNode nodeFromInputStream = mapper.toJsonNode(inputStream);
			assertThat(nodeFromInputStream).isEqualTo(nodeFromString);
		}

		try (Reader reader = new StringReader(jsonString)) {
			JsonNode nodeFromReader = mapper.toJsonNode(reader);
			assertThat(nodeFromReader).isEqualTo(nodeFromString);
		}
	}

	@Test
	void testToJsonNodeWithFile() throws IOException {
		Path tempFile = Files.createTempFile("test", ".json");
		String jsonContent = "{\"message\":\"hello from file\",\"number\":42}";
		Files.write(tempFile, jsonContent.getBytes(StandardCharsets.UTF_8));

		try {
			File file = tempFile.toFile();
			JsonNode nodeFromFile = mapper.toJsonNode(file);
			assertThat(nodeFromFile.get("message").asString()).isEqualTo("hello from file");
			assertThat(nodeFromFile.get("number").asInt()).isEqualTo(42);

			URL fileUrl = file.toURI().toURL();
			JsonNode nodeFromUrl = mapper.toJsonNode(fileUrl);
			assertThat(nodeFromUrl).isEqualTo(nodeFromFile);
		}
		finally {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	void testToJsonWithWriter() throws IOException {
		TestData data = new TestData("John", Optional.of("john@test.com"), Optional.empty());

		try (StringWriter writer = new StringWriter()) {
			mapper.toJson(data, writer);
			String json = writer.toString();
			assertThat(json).isEqualTo("{\"name\":\"John\",\"email\":\"john@test.com\",\"age\":null}");
		}
	}

	@Test
	void testFromJsonWithUnsupportedType() {
		Object unsupportedInput = new Date();

		assertThatThrownBy(() -> mapper.fromJson(unsupportedInput, String.class))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"42", "true", "\"hello\"", "3.14", "null"})
	void testPrimitiveTypes(String jsonValue) throws IOException {
		JsonNode node = mapper.toJsonNode(jsonValue);
		assertThat(node).isNotNull();

		String serialized = mapper.toJson(node);
		JsonNode roundTrip = mapper.toJsonNode(serialized);
		assertThat(roundTrip).isEqualTo(node);
	}

	@Test
	void testCollectionTypes() throws IOException {
		List<String> stringList = Arrays.asList("a", "b", "c");
		String json = mapper.toJson(stringList);
		assertThat(json).isEqualTo("[\"a\",\"b\",\"c\"]");

		@SuppressWarnings("unchecked")
		List<String> deserialized = mapper.fromJson(json, List.class);
		assertThat(deserialized).isEqualTo(stringList);

		Set<Integer> intSet = Set.of(1, 2, 3);
		String setJson = mapper.toJson(intSet);
		assertThat(setJson).isNotNull();
	}

	@Test
	void testMapTypes() throws IOException {
		Map<String, Object> map = Map.of(
				"string", "value",
				"number", 42,
				"boolean", true,
				"nested", Map.of("inner", "value")
		);

		String json = mapper.toJson(map);
		assertThat(json).isNotNull();

		@SuppressWarnings("unchecked")
		Map<String, Object> deserialized = mapper.fromJson(json, Map.class);
		assertThat(deserialized.get("string")).isEqualTo("value");
		assertThat(deserialized.get("number")).isEqualTo(42);
		assertThat(deserialized.get("boolean")).isEqualTo(true);
	}

	@Test
	public void testOptional() throws IOException {
		TestData data = new TestData("John", Optional.of("john@test.com"), Optional.empty());

		String json = mapper.toJson(data);
		assertThat(json).isEqualTo("{\"name\":\"John\",\"email\":\"john@test.com\",\"age\":null}");

		TestData deserialized = mapper.fromJson(json, TestData.class);
		assertThat(deserialized).isEqualTo(data);
	}

	@Test
	public void testJavaTime() throws Exception {
		LocalDateTime localDateTime = LocalDateTime.of(2000, 1, 1, 0, 0);
		ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));
		TimeData data = new TimeData(localDateTime, zonedDateTime);

		String json = mapper.toJson(data);
		assertThat("{\"localDate\":\"2000-01-01T00:00:00\",\"zoneDate\":\"2000-01-01T00:00:00Z\"}").isEqualTo(json);

		TimeData deserialized = mapper.fromJson(json, TimeData.class);
		assertThat(deserialized.localDate()).isEqualTo(data.localDate());
		assertThat(deserialized.zoneDate().toInstant()).isEqualTo(data.zoneDate().toInstant());
	}

	@Test
	public void testJodaWithJodaModule() throws Exception {
		ObjectMapper objectMapper = mapper.getObjectMapper();
		Set<String> registeredModules = getModuleNames(objectMapper.getRegisteredModules());
		assertThat(registeredModules.contains(JODA_MODULE.getModuleName())).isTrue();

		org.joda.time.DateTime jodaDateTime = new DateTime(2000, 1, 1, 0, 0, DateTimeZone.UTC);
		JodaData data = new JodaData("John", jodaDateTime);

		String json = mapper.toJson(data);
		assertThat("{\"name\":\"John\",\"jodaDate\":\"2000-01-01T00:00:00.000Z\"}").isEqualTo(json);

		JodaData deserialized = mapper.fromJson(json, JodaData.class);
		assertThat(deserialized.name()).isEqualTo(data.name());
		assertThat(deserialized.jodaDate()).isEqualTo(data.jodaDate());
	}

	@Test
	public void testJodaWithoutJodaModule() {
		ObjectMapper customMapper = JsonMapper.builder().build();
		JacksonJsonObjectMapper mapper = new JacksonJsonObjectMapper(customMapper);

		Set<String> registeredModules = getModuleNames(mapper.getObjectMapper().getRegisteredModules());
		assertThat(registeredModules.contains(JODA_MODULE.getModuleName())).isFalse();

		org.joda.time.DateTime jodaDateTime = new DateTime(2000, 1, 1, 0, 0, DateTimeZone.UTC);
		JodaData data = new JodaData("John", jodaDateTime);

		assertThatThrownBy(() -> mapper.toJson(data))
				.isInstanceOf(IOException.class);

		String json = "{\"name\":\"John\",\"jodaDate\":\"2000-01-01T00:00:00.000Z\"}";
		assertThatThrownBy(() -> mapper.fromJson(json, JodaData.class))
				.isInstanceOf(IOException.class);
	}

	private Set<String> getModuleNames(Collection<JacksonModule> modules) {
		return modules.stream()
				.map(JacksonModule::getModuleName)
				.collect(Collectors.toUnmodifiableSet());
	}

	private List<JacksonModule> collectWellKnownModulesIfAvailable() {
		return List.of(JODA_MODULE, KOTLIN_MODULE);
	}

	private record TestData(String name, Optional<String> email, Optional<Integer> age) {
	}

	private record TimeData(LocalDateTime localDate, ZonedDateTime zoneDate) {
	}

	private record JodaData(String name, org.joda.time.DateTime jodaDate) {
	}

}
