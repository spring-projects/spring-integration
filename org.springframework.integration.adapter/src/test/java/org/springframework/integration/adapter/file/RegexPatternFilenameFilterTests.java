/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.Test;

import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Mark Fisher
 */
public class RegexPatternFilenameFilterTests {

	@Test
	public void match() {
		File file = new File("/some/path/test.txt");
		RegexPatternFilenameFilter filter = new RegexPatternFilenameFilter();
		filter.setPattern(Pattern.compile("[a-z]+\\.txt"));
		assertTrue(filter.accept(file.getParentFile(), file.getName()));
	}

	@Test
	public void noMatch() {
		File file = new File("/some/path/Test.txt");
		RegexPatternFilenameFilter filter = new RegexPatternFilenameFilter();
		filter.setPattern(Pattern.compile("[a-z]+\\.txt"));
		assertFalse(filter.accept(file.getParentFile(), file.getName()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void patternNotSet() {
		File file = new File("/some/path/test.txt");
		RegexPatternFilenameFilter filter = new RegexPatternFilenameFilter();
		filter.accept(file.getParentFile(), file.getName());
	}

	@Test
	public void patternEditorInContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"regexPatternFilenameFilterTests.xml", this.getClass());
		FilenameFilter filter = (FilenameFilter) context.getBean("filter");
		File file = new File("/some/path/foo.txt");
		assertTrue(filter.accept(file.getParentFile(), file.getName()));
	}

	@Test
	public void invalidPatternSyntax() {
		try {
			new ClassPathXmlApplicationContext("invalidRegexPatternFilenameFilterTests.xml", this.getClass());
			throw new IllegalStateException("context creation should have failed");
		}
		catch (Exception e) {
			assertEquals(BeanCreationException.class, e.getClass());
			Throwable cause1 = e.getCause();
			assertEquals(TypeMismatchException.class, cause1.getClass());
			Throwable cause2 = cause1.getCause();
			assertEquals(PatternSyntaxException.class, cause2.getClass());
		}
	}

}
