/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.codec.Codec;
import org.springframework.integration.codec.CompositeCodec;

import static org.assertj.core.api.Assertions.assertThat;

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
