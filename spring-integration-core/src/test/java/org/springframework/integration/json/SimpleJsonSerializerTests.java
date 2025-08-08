/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
