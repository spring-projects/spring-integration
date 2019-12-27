/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.transformer.support;

/**
 * Pre-defined names and prefixes for Apache Avro related headers.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 */
public final class AvroHeaders {

	private AvroHeaders() {
	}

	/**
	 * The prefix for Apache Avro specific message headers.
	 */
	public static final String PREFIX = "avro_";

	/**
	 * The {@code SpecificRecord} type. By default it's the fully qualified
	 * SpecificRecord type but can be a key that is mapped to the actual type.
	 */
	public static final String TYPE = PREFIX + "type";

}
