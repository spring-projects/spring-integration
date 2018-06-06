/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.file.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * @author Gary Russell
 * @since 5.1
 *
 */
public class FileNameHelperTests {

	@Test
	public void testSimpleDefault() {
		FileNameHelper helper = new FileNameHelper();
		assertThat(helper.toTempFile("foo")).isEqualTo("foo.writing");
		assertThat(helper.excludeTemporaryMatches(null).matcher("foo").matches()).isTrue();
		assertThat(helper.excludeTemporaryMatches(null).matcher("foo.writing").matches()).isFalse();
	}

	@Test
	public void testSimpleSuffix() {
		FileNameHelper helper = FileNameHelper.defaultForSuffix(".bar");
		assertThat(helper.toTempFile("foo")).isEqualTo("foo.bar");
		assertThat(helper.excludeTemporaryMatches(null).matcher("foo").matches()).isTrue();
		assertThat(helper.excludeTemporaryMatches(null).matcher("foo.bar").matches()).isFalse();
	}

	@Test
	public void testSimplePrefix() {
		FileNameHelper helper = FileNameHelper.defaultForPrefix("bar.");
		assertThat(helper.toTempFile("foo")).isEqualTo("bar.foo");
		assertThat(helper.excludeTemporaryMatches(null).matcher("foo").matches()).isTrue();
		assertThat(helper.excludeTemporaryMatches(null).matcher("bar.foo").matches()).isFalse();
	}

	@Test
	public void testPathDefault() {
		FileNameHelper helper = new FileNameHelper();
		String directory = "foo" + File.separator + "bar" + File.separator;
		String path = directory + "baz";
		assertThat(helper.toTempFile(path)).isEqualTo(path + ".writing");
	}

	@Test
	public void testPathSuffix() {
		FileNameHelper helper = FileNameHelper.defaultForSuffix(".qux");
		String directory = "foo" + File.separator + "bar" + File.separator;
		String path = directory + "baz";
		assertThat(helper.toTempFile(path)).isEqualTo(path + ".qux");
	}

	@Test
	public void testPathPrefix() {
		FileNameHelper helper = FileNameHelper.defaultForPrefix("qux.");
		String directory = "foo" + File.separator + "bar" + File.separator;
		String path = directory + "baz";
		assertThat(helper.toTempFile(path)).isEqualTo(directory + "qux." + "baz");
	}

}
