/*
 * Copyright 2026-present the original author or authors.
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

/**
 * Pre-defined header names to be used for setting and/or retrieving CloudEvent attributes from a
 * {@link org.springframework.messaging.Message}.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public abstract class CloudEventHeaders {

	private CloudEventHeaders() {
	}

	public static final String PREFIX = "ce-";

	public static final String EVENT_ID = PREFIX + "id";

	public static final String EVENT_TIME = PREFIX + "time";

	public static final String EVENT_SOURCE = PREFIX + "source";

	public static final String EVENT_DATA_CONTENT_TYPE = PREFIX + "datacontenttype";

	public static final String EVENT_SUBJECT = PREFIX + "subject";

	public static final String EVENT_DATA_SCHEMA = PREFIX + "dataschema";

	public static final String EVENT_TYPE = PREFIX + "type";

}
