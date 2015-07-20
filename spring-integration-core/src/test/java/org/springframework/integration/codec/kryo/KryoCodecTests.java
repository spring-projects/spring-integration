/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * @since 1.0
 */
public class KryoCodecTests {

	@Test
	public void testStringSerialization() throws IOException {
		String str = "hello";
		PojoCodec serializer = new PojoCodec();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		serializer.serialize(str, bos);

		String s2 = (String)serializer.deserialize(bos.toByteArray(), String.class);
		assertEquals(str, s2);
	}

	@Test
	public void testSerializationWithStreams() throws IOException {
		String str = "hello";
		File file = new File("test.ser");
		PojoCodec serializer = new PojoCodec();
		FileOutputStream fos = new FileOutputStream(file);
		serializer.serialize(str, fos);
		fos.close();

		FileInputStream fis = new FileInputStream(file);
		String s2 = (String) serializer.deserialize(fis, String.class);
		file.delete();
		assertEquals(str, s2);
	}

	@Test
	public void testPojoSerialization() throws IOException {
		PojoCodec serializer = new PojoCodec();
		SomeClassWithNoDefaultConstructors foo = new SomeClassWithNoDefaultConstructors("foo", 123);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(foo, bos);
		Object foo2 = serializer.deserialize(bos.toByteArray(), SomeClassWithNoDefaultConstructors.class);
		assertEquals(foo, foo2);
	}

	static class SomeClassWithNoDefaultConstructors {

		private String val1;

		private int val2;

		public SomeClassWithNoDefaultConstructors(String val1, int val2) {
			this.val1 = val1;
			this.val2 = val2;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof SomeClassWithNoDefaultConstructors)) {
				return false;
			}
			SomeClassWithNoDefaultConstructors that = (SomeClassWithNoDefaultConstructors) other;
			return (this.val1.equals(that.val1) && val2 == that.val2);
		}

		@Override
		public int hashCode() {
			int result = val1.hashCode();
			result = 31 * result + val2;
			return result;
		}
	}

	@Test
	public void testPrimitiveSerialization() throws IOException {
		PojoCodec serializer = new PojoCodec();

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(true, bos);
		boolean b = (Boolean) serializer.deserialize(bos.toByteArray(), Boolean.class);
		assertEquals(true, b);
		b = (Boolean) serializer.deserialize(bos.toByteArray(), boolean.class);
		assertEquals(true, b);

		bos = new ByteArrayOutputStream();
		serializer.serialize(3.14159, bos);

		double d = (Double) serializer.deserialize(bos.toByteArray(), double.class);
		assertEquals(3.14159, d, 0.00001);

		bos = new ByteArrayOutputStream();
		serializer.serialize(new Double(3.14159), bos);

		d = (Double) serializer.deserialize(bos.toByteArray(), Double.class);
		assertEquals(3.14159, d, 0.00001);

	}

	@Test
	public void testMapSerialization() throws IOException {
		PojoCodec serializer = new PojoCodec();
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("one", 1);
		map.put("two", 2);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(map, bos);
		Map<?, ?> m2 = (Map<?, ?>) serializer.deserialize(bos.toByteArray(), HashMap.class);
		assertEquals(2, m2.size());
		assertEquals(1, m2.get("one"));
		assertEquals(2, m2.get("two"));
	}

	@Test
	public void testComplexObjectSerialization() throws IOException {
		PojoCodec serializer = new PojoCodec();
		Foo foo = new Foo();
		foo.put("one", 1);
		foo.put("two", 2);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(foo, bos);

		Foo foo2 = (Foo) serializer.deserialize(bos.toByteArray(), Foo.class);
		assertEquals(1, foo2.get("one"));
		assertEquals(2, foo2.get("two"));
	}



	static class Foo {

		private Map<Object, Object> map;

		public Foo() {
			map = new HashMap<Object, Object>();
		}

		public void put(Object key, Object value) {
			map.put(key, value);
		}

		public Object get(Object key) {
			return map.get(key);
		}
	}
}
