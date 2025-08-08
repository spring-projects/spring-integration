/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
