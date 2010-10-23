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
public class SimplePatternFileListFilterTest {

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
