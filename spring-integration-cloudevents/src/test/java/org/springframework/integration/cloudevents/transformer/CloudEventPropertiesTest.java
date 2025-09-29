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

package org.springframework.integration.cloudevents.transformer;

import java.net.URI;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CloudEventPropertiesTest {

	private CloudEventProperties properties;

	@BeforeEach
	void setUp() {
		this.properties = new CloudEventProperties();
	}

	@Test
	void defaultValues() {
		assertThat(this.properties.getId()).isEqualTo("");
		assertThat(this.properties.getSource()).isEqualTo(URI.create(""));
		assertThat(this.properties.getType()).isEqualTo("");
		assertThat(this.properties.getDataContentType()).isNull();
		assertThat(this.properties.getDataSchema()).isNull();
		assertThat(this.properties.getSubject()).isNull();
		assertThat(this.properties.getTime()).isNull();
	}

	@Test
	void setAndGetId() {
		String testId = "test-event-id-123";
		this.properties.setId(testId);
		assertThat(this.properties.getId()).isEqualTo(testId);
	}

	@Test
	void setAndGetSource() {
		URI testSource = URI.create("https://example.com/source");
		this.properties.setSource(testSource);
		assertThat(this.properties.getSource()).isEqualTo(testSource);
	}

	@Test
	void setAndGetType() {
		String testType = "com.example.event.type";
		this.properties.setType(testType);
		assertThat(this.properties.getType()).isEqualTo(testType);
	}

	@Test
	void setAndGetDataContentType() {
		String testContentType = "application/json";
		this.properties.setDataContentType(testContentType);
		assertThat(this.properties.getDataContentType()).isEqualTo(testContentType);
	}

	@Test
	void setAndGetDataSchema() {
		URI testSchema = URI.create("https://example.com/schema");
		this.properties.setDataSchema(testSchema);
		assertThat(this.properties.getDataSchema()).isEqualTo(testSchema);
	}

	@Test
	void setAndGetSubject() {
		String testSubject = "test-subject";
		this.properties.setSubject(testSubject);
		assertThat(this.properties.getSubject()).isEqualTo(testSubject);
	}

	@Test
	void setAndGetTime() {
		OffsetDateTime testTime = OffsetDateTime.now();
		this.properties.setTime(testTime);
		assertThat(this.properties.getTime()).isEqualTo(testTime);
	}

	@Test
	void setNullValues() {
		this.properties.setDataContentType(null);
		assertThat(this.properties.getDataContentType()).isNull();

		this.properties.setDataSchema(null);
		assertThat(this.properties.getDataSchema()).isNull();

		this.properties.setSubject(null);
		assertThat(this.properties.getSubject()).isNull();

		this.properties.setTime(null);
		assertThat(this.properties.getTime()).isNull();
	}

	@Test
	void setEmptyStringValues() {
		this.properties.setId("");
		assertThat(this.properties.getId()).isEqualTo("");

		this.properties.setType("");
		assertThat(this.properties.getType()).isEqualTo("");

		this.properties.setDataContentType("");
		assertThat(this.properties.getDataContentType()).isEqualTo("");

		this.properties.setSubject("");
		assertThat(this.properties.getSubject()).isEqualTo("");
	}

	@Test
	void completeCloudEventProperties() {
		String id = "complete-event-123";
		URI source = URI.create("https://example.com/events");
		String type = "com.example.user.created";
		String dataContentType = "application/json";
		URI dataSchema = URI.create("https://example.com/schemas/user");
		String subject = "user/123";
		OffsetDateTime time = OffsetDateTime.now();

		this.properties.setId(id);
		this.properties.setSource(source);
		this.properties.setType(type);
		this.properties.setDataContentType(dataContentType);
		this.properties.setDataSchema(dataSchema);
		this.properties.setSubject(subject);
		this.properties.setTime(time);

		assertThat(this.properties.getId()).isEqualTo(id);
		assertThat(this.properties.getSource()).isEqualTo(source);
		assertThat(this.properties.getType()).isEqualTo(type);
		assertThat(this.properties.getDataContentType()).isEqualTo(dataContentType);
		assertThat(this.properties.getDataSchema()).isEqualTo(dataSchema);
		assertThat(this.properties.getSubject()).isEqualTo(subject);
		assertThat(this.properties.getTime()).isEqualTo(time);
	}

}
