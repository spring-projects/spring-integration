/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.groovy.config;

import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.scripting.RefreshableResourceScriptSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
public class RefreshableResourceScriptSourceTests {

	@Test
	public void testGetScriptAsString() throws Exception {
		RefreshableResourceScriptSource source =
				new RefreshableResourceScriptSource(new ByteArrayResource("foo".getBytes()), 1000);
		assertThat(source.getScriptAsString()).isEqualTo("foo");
	}

	@Test
	public void testIsModified() {
		RefreshableResourceScriptSource source =
				new RefreshableResourceScriptSource(new ByteArrayResource("foo".getBytes()), 1000);
		assertThat(source.isModified()).isFalse();
	}

	@Test
	public void testIsModifiedZeroDelay() throws Exception {
		RefreshableResourceScriptSource source =
				new RefreshableResourceScriptSource(
						new ByteArrayResource("foo".getBytes()) {

							@Override
							public long lastModified() {
								return System.currentTimeMillis();
							}

						}, 0);
		Thread.sleep(100L);
		assertThat(source.isModified()).isTrue();
		assertThat(source.getScriptAsString()).isEqualTo("foo");
	}

	@Test
	public void testIsModifiedInfiniteDelay() throws Exception {
		RefreshableResourceScriptSource source =
				new RefreshableResourceScriptSource(
						new ByteArrayResource("foo".getBytes()) {

							@Override
							public long lastModified() {
								return System.currentTimeMillis();
							}

						}, -1);
		assertThat(source.isModified()).isFalse();
		assertThat(source.getScriptAsString()).isEqualTo("foo");
	}

	@Test
	public void testSuggestedClassName() {
		RefreshableResourceScriptSource source =
				new RefreshableResourceScriptSource(
						new ByteArrayResource("foo".getBytes()) {

							@Override
							public String getFilename() throws IllegalStateException {
								return "Foo";
							}

						}, 1000);
		assertThat(source.suggestedClassName()).isEqualTo("Foo");
	}

}
