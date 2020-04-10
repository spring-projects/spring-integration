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

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.MessageHeaders;

import io.cloudevents.extensions.ExtensionFormat;
import io.cloudevents.format.BinaryMarshaller;
import io.cloudevents.format.StructuredMarshaller;
import io.cloudevents.format.Wire;
import io.cloudevents.format.builder.EventStep;
import io.cloudevents.fun.DataMarshaller;
import io.cloudevents.json.Json;
import io.cloudevents.v1.Accessor;
import io.cloudevents.v1.AttributesImpl;

/**
 * A Cloud Events  general purpose marshallers factory.
 *
 * @author Artem Bilan
 *
 * @since 5.3
 */
public final class Marshallers {

	private static final Map<String, String> NO_HEADERS = new HashMap<>();

	/**
	 * Builds a Binary Content Mode marshaller to marshal cloud events as JSON for
	 * any Transport Binding.
	 * @param <T> The data type
	 * @return a builder to provide the {@link io.cloudevents.CloudEvent} and marshal as JSON
	 * @see BinaryMarshaller
	 */
	public static <T> EventStep<AttributesImpl, T, byte[], String> binary() {
		return binary(Json::binaryMarshal);
	}

	/**
	 * Builds a Binary Content Mode marshaller to marshal cloud events as a {@code byte[]} for
	 * any Transport Binding.
	 * The data marshalling is based on the provided {@link DataMarshaller}.
	 * @param marshaller the {@link DataMarshaller} for cloud event payload.
	 * @param <T> The data type
	 * @return a builder to provide the {@link io.cloudevents.CloudEvent} and marshal as JSON
	 * @see BinaryMarshaller
	 */
	public static <T> EventStep<AttributesImpl, T, byte[], String> binary(
			DataMarshaller<byte[], T, String> marshaller) {

		return BinaryMarshaller.<AttributesImpl, T, byte[], String>builder()
				.map(AttributesImpl::marshal)
				.map(Accessor::extensionsOf)
				.map(ExtensionFormat::marshal)
				.map(HeaderMapper::map)
				.map(marshaller)
				.builder(Wire::new);
	}

	/**
	 * Builds a Structured Content Mode marshaller to marshal cloud event as JSON for
	 * any Transport Binding.
	 * @param <T> The data type
	 * @return a builder to provide the {@link io.cloudevents.CloudEvent} and marshal as JSON
	 * @see StructuredMarshaller
	 */
	public static <T> EventStep<AttributesImpl, T, byte[], String> structured() {
		return StructuredMarshaller.<AttributesImpl, T, byte[], String>
				builder()
				.mime(MessageHeaders.CONTENT_TYPE, "application/cloudevents+json")
				.map((event) -> Json.binaryMarshal(event, NO_HEADERS))
				.skip();
	}

	private Marshallers() {

	}

}
