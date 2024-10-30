/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.integration.json;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.json.JsonObjectMapperProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
class SimpleJsonSerializerTests {

	@Test
	void verifySimpleJsonSerializerAgainstSimpleContent() throws Exception {
		Foo foo = new Foo("foo");
		String json = SimpleJsonSerializer.toJson(foo, "fileInfo");
		Foo fooOut = JsonObjectMapperProvider.newInstance().fromJson(json, Foo.class);
		assertThat(fooOut.bool).isEqualTo(Boolean.TRUE);
		assertThat(fooOut.bar).isEqualTo(42L);
		assertThat(fooOut.foo).isEqualTo("bar");
		assertThat(fooOut.dub).isEqualTo(1.6);
		assertThat(fooOut.fileInfo).isNull();
	}

	@Test
	void verifySimpleJsonSerializerAgainstDollarContent() throws Exception {
		Foo foo = new Foo("some content with $");
		String json = SimpleJsonSerializer.toJson(foo);
		Foo fooOut = JsonObjectMapperProvider.newInstance().fromJson(json, Foo.class);
		assertThat(fooOut.fileInfo).isEqualTo("some content with $");
	}

	static class Foo {

		private final String foo = "bar";

		private final long bar = 42L;

		private final double dub = 1.6;

		private final boolean bool = true;

		private String fileInfo;

		Foo() {
		}

		Foo(String info) {
			this.fileInfo = info;
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

		public void setFileInfo(String fileInfo) {
			this.fileInfo = fileInfo;
		}

		public String getFileInfo() {
			return this.fileInfo;
		}

		public String getPermissions() {
			throw new UnsupportedOperationException("Permissions are not supported");
		}

	}

}
