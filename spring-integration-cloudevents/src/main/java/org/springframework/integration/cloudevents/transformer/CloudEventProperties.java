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

import org.jspecify.annotations.Nullable;

import org.springframework.integration.cloudevents.CloudEventsHeaders;

/**
 * Configuration properties for CloudEvent metadata and formatting.
 * <p>
 * This class provides configurable properties for CloudEvent creation, including
 * required attributes (id, source, type) and optional attributes (subject, time, dataContentType, dataSchema).
 * It also supports customization of the CloudEvent header prefix for integration with different systems.
 * <p>
 * All properties have defaults and can be configured as needed:
 * <ul>
 *   <li>Required attributes default to empty strings/URIs</li>
 *   <li>Optional attributes default to null</li>
 *   <li>CloudEvent prefix defaults to standard "ce-" format</li>
 * </ul>
 *
 * @author Glenn Renfro
 *
 * @since 7.0
 */
public class CloudEventProperties {

	private String id = "";

	private URI source = URI.create("");

	private String type = "";

	private @Nullable String dataContentType;

	private @Nullable URI dataSchema;

	private @Nullable String subject;

	private @Nullable OffsetDateTime time;

	private String cePrefix = CloudEventsHeaders.CE_PREFIX;

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public URI getSource() {
		return this.source;
	}

	public void setSource(URI source) {
		this.source = source;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public @Nullable String getDataContentType() {
		return this.dataContentType;
	}

	public void setDataContentType(@Nullable String dataContentType) {
		this.dataContentType = dataContentType;
	}

	public @Nullable URI getDataSchema() {
		return this.dataSchema;
	}

	public void setDataSchema(@Nullable URI dataSchema) {
		this.dataSchema = dataSchema;
	}

	public @Nullable String getSubject() {
		return this.subject;
	}

	public void setSubject(@Nullable String subject) {
		this.subject = subject;
	}

	public @Nullable OffsetDateTime getTime() {
		return this.time;
	}

	public void setTime(@Nullable OffsetDateTime time) {
		this.time = time;
	}

	public String getCePrefix() {
		return this.cePrefix;
	}

	public void setCePrefix(String cePrefix) {
		this.cePrefix = cePrefix;
	}

}
