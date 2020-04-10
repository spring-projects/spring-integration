/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.support.cloudevents;

/**
 * Message headers for basic cloud event attributes.
 * These headers might be remapped to respective attributes/headers
 * in the target protocol binder.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public final class CloudEventHeaders {

	private CloudEventHeaders() {
	}

	/**
	 * Header prefix as a {@value PREFIX} for cloud event attributes.
	 */
	public static final String PREFIX = "ce_";

	/**
	 * The header name for cloud event {@code id} attribute.
	 */
	public static final String ID = PREFIX + "id";

	/**
	 * The header name for cloud event {@code source} attribute.
	 */
	public static final String SOURCE = PREFIX + "source";

	/**
	 * The header name for cloud event {@code specversion} attribute.
	 */
	public static final String SPEC_VERSION = PREFIX + "specversion";

	/**
	 * The header name for cloud event {@code type} attribute.
	 */
	public static final String TYPE = PREFIX + "type";

	/**
	 * The header name for cloud event {@code datacontenttype} attribute.
	 */
	public static final String DATA_CONTENT_TYPE = PREFIX + "datacontenttype";

	/**
	 * The header name for cloud event {@code dataschema} attribute.
	 */
	public static final String DATA_SCHEMA = PREFIX + "dataschema";

	/**
	 * The header name for cloud event {@code subject} attribute.
	 */
	public static final String SUBJECT = PREFIX + "subject";

	/**
	 * The header name for cloud event {@code time} attribute.
	 */
	public static final String TIME = PREFIX + "time";


}
