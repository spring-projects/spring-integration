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
public class AntPathFileListFilterTest {

	@Test
	public void shouldMatchExactly() {
		assertThat(new AntPathFileListFilter("foo/bar").accept(new File("foo/bar")), is(true));
	}

	@Test
	public void shouldMatchQuestionMark() {
		assertThat(new AntPathFileListFilter("*/bar").accept(new File("foo/bar")), is(true));
	}
	
	@Test
	public void shouldMatchWildcard() {
		assertThat(new AntPathFileListFilter("foo/ba?").accept(new File("foo/bar")), is(true));
	}

}
