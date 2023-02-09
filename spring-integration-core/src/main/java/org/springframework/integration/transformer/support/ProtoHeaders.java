/*
 * Copyright 2023 the original author or authors.
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
 * Pre-defined names and prefixes for Protocol Buffers related headers.
 *
 * @author Christian Tzolov
 *
 * @since 6.1
 */
public final class ProtoHeaders {

	private ProtoHeaders() {
	}

	/**
	 * The prefix for Protocol Buffers specific message headers.
	 */
	public static final String PREFIX = "proto_";

	/**
	 * The {@code com.google.protobuf.Message} type. By default, it's the fully qualified
	 * {@code com.google.protobuf.Message} type but can be a key that is mapped to the actual type.
	 */
	public static final String TYPE = PREFIX + "type";

}
