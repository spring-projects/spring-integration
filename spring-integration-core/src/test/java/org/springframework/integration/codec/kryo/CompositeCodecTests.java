/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.codec.kryo;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.CompositeCodec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author David Turanski
 * @author Glenn Renfro
 * @since 4.2
 */
public class CompositeCodecTests {

	@Test
	void testWithCodecDelegates() throws IOException {
		Codec codec = getFullyQualifiedCodec();
		SomeClassWithNoDefaultConstructors inputInstance = new SomeClassWithNoDefaultConstructors("hello", 123);
		SomeClassWithNoDefaultConstructors outputInstance = codec.decode(
				codec.encode(inputInstance),
				SomeClassWithNoDefaultConstructors.class);
		assertThat(outputInstance).isEqualTo(inputInstance);
	}

	@Test
	void testWithCodecDefault() throws IOException {
		Codec codec = getFullyQualifiedCodec();
		AnotherClassWithNoDefaultConstructors inputInstance = new AnotherClassWithNoDefaultConstructors("hello", 123);
		AnotherClassWithNoDefaultConstructors outputInstance = codec.decode(
				codec.encode(inputInstance),
				AnotherClassWithNoDefaultConstructors.class);
		assertThat(outputInstance).isEqualTo(inputInstance);
	}

	@Test
	void testWithUnRegisteredClass() throws IOException {
		// Verify that the default encodes and decodes properly
		Codec codec = onlyDefaultCodec();
		SomeClassWithNoDefaultConstructors inputInstance = new SomeClassWithNoDefaultConstructors("hello", 123);
		SomeClassWithNoDefaultConstructors outputInstance = codec.decode(
				codec.encode(inputInstance),
				SomeClassWithNoDefaultConstructors.class);
		assertThat(outputInstance).isEqualTo(inputInstance);

		// Verify that an exception is thrown if an unknown type is to be encoded.
		assertThatIllegalArgumentException().isThrownBy(() -> codec.decode(
				codec.encode(inputInstance),
				AnotherClassWithNoDefaultConstructors.class));
	}

	private static Codec getFullyQualifiedCodec() {
		Map<Class<?>, Codec> codecs = Map.of(SomeClassWithNoDefaultConstructors.class, new PojoCodec(
				new KryoClassListRegistrar(SomeClassWithNoDefaultConstructors.class)));
		return new CompositeCodec(codecs, new PojoCodec(
				new KryoClassListRegistrar(AnotherClassWithNoDefaultConstructors.class)));
	}

	private static Codec onlyDefaultCodec() {
		PojoCodec pojoCodec = new PojoCodec();
		Map<Class<?>, Codec> codecs = Map.of(java.util.Date.class, pojoCodec);
		return new CompositeCodec(codecs, new PojoCodec(
				new KryoClassListRegistrar(SomeClassWithNoDefaultConstructors.class)));
	}

	private record SomeClassWithNoDefaultConstructors(String val1, int val2) { }

	private record AnotherClassWithNoDefaultConstructors(String val1, int val2) { }

}
