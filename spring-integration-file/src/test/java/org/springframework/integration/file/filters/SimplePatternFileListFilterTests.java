/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 *
 * Minimal test set to ensure AntPathMatcher is used correctly.
 */
public class SimplePatternFileListFilterTests {

	@Test
	public void shouldMatchExactly() {
		assertThat(new SimplePatternFileListFilter("bar").accept(new File("bar"))).isTrue();
	}

	@Test
	public void shouldMatchQuestionMark() {
		assertThat(new SimplePatternFileListFilter("*bar").accept(new File("bar"))).isTrue();
	}

	@Test
	public void shouldMatchWildcard() {
		assertThat(new SimplePatternFileListFilter("ba?").accept(new File("bar"))).isTrue();
	}

}
