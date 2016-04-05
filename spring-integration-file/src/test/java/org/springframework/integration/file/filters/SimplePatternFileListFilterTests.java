/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.file.filters;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
 *
 * Minimal test set to ensure AntPathMatcher is used correctly.
 */
public class SimplePatternFileListFilterTests {

	@Test
	public void shouldMatchExactly() {
		assertThat(new SimplePatternFileListFilter("bar").accept(new File("bar")), is(true));
	}

	@Test
	public void shouldMatchQuestionMark() {
		assertThat(new SimplePatternFileListFilter("*bar").accept(new File("bar")), is(true));
	}

	@Test
	public void shouldMatchWildcard() {
		assertThat(new SimplePatternFileListFilter("ba?").accept(new File("bar")), is(true));
	}

}
