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

package org.springframework.integration.cloudevents;

/**
 * Pre-defined header names to be used for setting and/or retrieving {@link io.cloudevents.CloudEvent}
 * attributes to/from a {@link org.springframework.messaging.Message}.
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 7.1
 */
public final class CloudEventHeaders {

	/**
	 * The prefix used for CloudEvent attribute headers.
	 * <p>Value: {@value}
	 */
	public static final String PREFIX = "ce-";

	/**
	 * Header name for the CloudEvent {@code id} attribute.
	 * <p>The unique identifier for the CloudEvent.
	 * <p>Value: {@value}
	 */
	public static final String EVENT_ID = PREFIX + "id";

	/**
	 * Header name for the CloudEvent {@code time} attribute.
	 * <p>The timestamp of when the occurrence happened.
	 * <p>Value: {@value}
	 */
	public static final String EVENT_TIME = PREFIX + "time";

	/**
	 * Header name for the CloudEvent {@code source} attribute.
	 * <p>Identify the context in which an event happened.
	 * <p>Value: {@value}
	 */
	public static final String EVENT_SOURCE = PREFIX + "source";

	/**
	 * Header name for the CloudEvent {@code datacontenttype} attribute.
	 * <p>The content type of the CloudEvent data value.
	 * <p>Value: {@value}
	 */
	public static final String EVENT_DATA_CONTENT_TYPE = PREFIX + "datacontenttype";

	/**
	 * Header name for the CloudEvent {@code subject} attribute.
	 * <p>Describe the subject of the event in the context of the event producer.
	 * <p>Value: {@value}
	 */
	public static final String EVENT_SUBJECT = PREFIX + "subject";

	/**
	 * Header name for the CloudEvent {@code dataschema} attribute.
	 * <p>Identify the schema that the data adheres to.
	 * <p>Value: {@value}
	 */
	public static final String EVENT_DATA_SCHEMA = PREFIX + "dataschema";

	/**
	 * Header name for the CloudEvent {@code type} attribute.
	 * <p>Describe the type of event related to the originating occurrence.
	 * <p>Value: {@value}
	 */
	public static final String EVENT_TYPE = PREFIX + "type";

	private CloudEventHeaders() {
	}

}
