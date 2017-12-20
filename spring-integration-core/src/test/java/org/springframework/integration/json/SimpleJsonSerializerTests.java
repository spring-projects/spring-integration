/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.json;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.integration.support.json.JsonObjectMapperProvider;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class SimpleJsonSerializerTests {

	@Test
	public void test() throws Exception {
		Foo foo = new Foo("foo");
		String json = SimpleJsonSerializer.toJson(foo, "fileInfo");
		Foo fooOut = JsonObjectMapperProvider.newInstance().fromJson(json, Foo.class);
		assertThat(fooOut.bool, equalTo(Boolean.TRUE));
		assertThat(fooOut.bar, equalTo(42L));
		assertThat(fooOut.foo, equalTo("bar"));
		assertThat(fooOut.dub, equalTo(1.6));
		assertNull(fooOut.fileInfo);
	}

	public static class Foo {

		private final String foo = "bar";

		private final long bar = 42L;

		private final double dub = 1.6;

		private final boolean bool = true;

		private String fileInfo;

		public Foo() {
			super();
		}

		public Foo(String info) {
			this.fileInfo = "foo";
		}

		public String getFoo() {
			return this.foo;
		}

		public long getBar() {
			return this.bar;
		}

		public double getDub() {
			return this.dub;
		}

		public boolean isBool() {
			return this.bool;
		}

		public String fileInfo() {
			return this.fileInfo;
		}

		public String getPermissions() {
			throw new UnsupportedOperationException("Permissions are not supported");
		}

	}

}
