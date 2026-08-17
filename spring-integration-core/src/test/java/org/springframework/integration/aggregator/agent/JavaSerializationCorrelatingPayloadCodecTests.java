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
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import org.springframework.integration.aggregator.agent.grpc.SerializedObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link JavaSerializationCorrelatingPayloadCodec}.
 *
 * @author OpenAI
 *
 * @since 7.2
 */
class JavaSerializationCorrelatingPayloadCodecTests {

	private final JavaSerializationCorrelatingPayloadCodec codec =
			new JavaSerializationCorrelatingPayloadCodec();

	@Test
	void roundTripsJdkPayloadWithDefaultFilter() {
		SerializedObject encoded = this.codec.encode(List.of("one", "two"));

		assertThat(this.codec.decode(encoded, JavaSerializationCorrelatingPayloadCodec.defaultFilter()))
				.isEqualTo(List.of("one", "two"));
		assertThat(encoded.getClassName()).contains("ImmutableCollections");
		assertThat(encoded.getContentType())
				.isEqualTo(JavaSerializationCorrelatingPayloadCodec.CONTENT_TYPE);
	}

	@Test
	void roundTripsApplicationPayloadWithExplicitFilter() {
		NestedPayload payload = new NestedPayload("test", List.of(1, 2, 3));

		Object decoded = this.codec.decode(this.codec.encode(payload), info -> ObjectInputFilter.Status.ALLOWED);

		assertThat(decoded).isEqualTo(payload);
	}

	@Test
	void handlesNullPayload() {
		SerializedObject encoded = this.codec.encode(null);

		assertThat(encoded.getNullValue()).isTrue();
		assertThat(this.codec.decode(encoded, JavaSerializationCorrelatingPayloadCodec.defaultFilter())).isNull();
	}

	@Test
	void rejectsNonSerializablePayload() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.codec.encode(new Object()))
				.withMessageContaining("must implement Serializable");
	}

	@Test
	void configuredFilterRejectsPayload() {
		SerializedObject encoded = this.codec.encode(new NestedPayload("test", List.of()));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.codec.decode(encoded, info -> ObjectInputFilter.Status.REJECTED))
				.withMessageContaining("Failed to deserialize");
	}

	@Test
	void rejectsCorruptedPayload() {
		SerializedObject corrupted = this.codec.encode("test").toBuilder()
				.setData(ByteString.copyFromUtf8("not-java-serialization"))
				.build();

		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.codec.decode(corrupted,
						JavaSerializationCorrelatingPayloadCodec.defaultFilter()))
				.withMessageContaining("Failed to deserialize");
	}

	private record NestedPayload(String name, List<Integer> values) implements Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

	}

}
