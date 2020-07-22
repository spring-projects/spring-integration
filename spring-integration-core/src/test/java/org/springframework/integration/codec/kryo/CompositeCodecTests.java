/*
 * Copyright 2015-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.CompositeCodec;

/**
 * @author David Turanski
 * @since 4.2
 */
public class CompositeCodecTests {

	private Codec codec;


	@Before
	public void setup() {
		Map<Class<?>, Codec> codecs = new HashMap<>();
		this.codec = new CompositeCodec(codecs, new PojoCodec(
				new KryoClassListRegistrar(SomeClassWithNoDefaultConstructors.class)));
	}

	@Test
	public void testPojoSerialization() throws IOException {
		SomeClassWithNoDefaultConstructors foo = new SomeClassWithNoDefaultConstructors("hello", 123);
		SomeClassWithNoDefaultConstructors foo2 = this.codec.decode(
				this.codec.encode(foo),
				SomeClassWithNoDefaultConstructors.class);
		assertThat(foo2).isEqualTo(foo);
	}

	static class SomeClassWithNoDefaultConstructors {

		private String val1;

		private int val2;

		SomeClassWithNoDefaultConstructors(String val1, int val2) {
			this.val1 = val1;
			this.val2 = val2;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof SomeClassWithNoDefaultConstructors)) {
				return false;
			}
			SomeClassWithNoDefaultConstructors that = (SomeClassWithNoDefaultConstructors) other;
			return (this.val1.equals(that.val1) && this.val2 == that.val2);
		}

		@Override
		public int hashCode() {
			int result = this.val1.hashCode();
			result = 31 * result + this.val2;
			return result;
		}

	}

}
