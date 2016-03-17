/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.codec.kryo;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author David Turanski
 * @since 4.2
 */
public class KryoCodecTests {

	@Test
	public void testStringSerialization() throws IOException {
		String str = "hello";
		PojoCodec codec = new PojoCodec();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		codec.encode(str, bos);

		String s2 = codec.decode(bos.toByteArray(), String.class);
		assertEquals(str, s2);
	}

	@Test
	public void testSerializationWithStreams() throws IOException {
		String str = "hello";
		File file = new File("test.ser");
		PojoCodec codec = new PojoCodec();
		FileOutputStream fos = new FileOutputStream(file);
		codec.encode(str, fos);
		fos.close();

		FileInputStream fis = new FileInputStream(file);
		String s2 = codec.decode(fis, String.class);
		file.delete();
		assertEquals(str, s2);
	}

	@Test
	public void testPojoSerialization() throws IOException {
		PojoCodec codec = new PojoCodec();
		SomeClassWithNoDefaultConstructors foo = new SomeClassWithNoDefaultConstructors("foo", 123);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		codec.encode(foo, bos);
		Object foo2 = codec.decode(bos.toByteArray(), SomeClassWithNoDefaultConstructors.class);
		assertEquals(foo, foo2);
	}

	@Test
	public void testPrimitiveSerialization() throws IOException {
		PojoCodec codec = new PojoCodec();

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		codec.encode(true, bos);
		boolean b = codec.decode(bos.toByteArray(), Boolean.class);
		assertEquals(true, b);
		b = codec.decode(bos.toByteArray(), boolean.class);
		assertEquals(true, b);

		bos = new ByteArrayOutputStream();
		codec.encode(3.14159, bos);

		double d = codec.decode(bos.toByteArray(), double.class);
		assertEquals(3.14159, d, 0.00001);

		bos = new ByteArrayOutputStream();
		codec.encode(3.14159, bos);

		d = codec.decode(bos.toByteArray(), Double.class);
		assertEquals(3.14159, d, 0.00001);

	}

	@Test
	public void testMapSerialization() throws IOException {
		PojoCodec codec = new PojoCodec();
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("one", 1);
		map.put("two", 2);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		codec.encode(map, bos);
		Map<?, ?> m2 = (Map<?, ?>) codec.decode(bos.toByteArray(), HashMap.class);
		assertEquals(2, m2.size());
		assertEquals(1, m2.get("one"));
		assertEquals(2, m2.get("two"));
	}

	@Test
	public void testComplexObjectSerialization() throws IOException {
		PojoCodec codec = new PojoCodec();
		Foo foo = new Foo();
		foo.put("one", 1);
		foo.put("two", 2);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		codec.encode(foo, bos);

		Foo foo2 = codec.decode(bos.toByteArray(), Foo.class);
		assertEquals(1, foo2.get("one"));
		assertEquals(2, foo2.get("two"));
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

	static class Foo {

		private Map<Object, Object> map;

		Foo() {
			map = new HashMap<Object, Object>();
		}

		public void put(Object key, Object value) {
			this.map.put(key, value);
		}

		public Object get(Object key) {
			return this.map.get(key);
		}

	}

}
