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
