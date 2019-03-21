/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.scripting.RefreshableResourceScriptSource;

/**
 * @author Dave Syer
 * @author Artem Bilan
 */
public class RefreshableResourceScriptSourceTests {

	@Test
	public void testGetScriptAsString() throws Exception {
		RefreshableResourceScriptSource source =
				new RefreshableResourceScriptSource(new ByteArrayResource("foo".getBytes()), 1000);
		assertEquals("foo", source.getScriptAsString());
	}

	@Test
	public void testIsModified() {
		RefreshableResourceScriptSource source =
				new RefreshableResourceScriptSource(new ByteArrayResource("foo".getBytes()), 1000);
		assertFalse(source.isModified());
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
		assertTrue(source.isModified());
		assertEquals("foo", source.getScriptAsString());
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
		assertFalse(source.isModified());
		assertEquals("foo", source.getScriptAsString());
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
		assertEquals("Foo", source.suggestedClassName());
	}

}
