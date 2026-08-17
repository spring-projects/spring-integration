/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.aggregator.agent;

import java.io.ObjectInputFilter;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.aggregator.agent.grpc.SerializedObject;

/**
 * Strategy for encoding message payloads transferred through the correlating agent port.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
public interface CorrelatingPayloadCodec {

	/**
	 * Encode a payload for transport.
	 * @param payload the payload, or {@code null}
	 * @return the transport value
	 */
	SerializedObject encode(@Nullable Object payload);

	/**
	 * Decode a transported payload.
	 * @param payload the transport value
	 * @param filter the mandatory deserialization filter
	 * @return the decoded payload, or {@code null}
	 */
	@Nullable
	Object decode(SerializedObject payload, ObjectInputFilter filter);

}
