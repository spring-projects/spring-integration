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

package org.springframework.integration.file;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class PatternMatchingFileListFilterTests {

	@Test
	public void matchSingleFile() {
		File[] files = new File[] {new File("/some/path/test.txt")};
		Pattern pattern = Pattern.compile("[a-z]+\\.txt");
		RegexPatternFileListFilter filter = new RegexPatternFileListFilter(pattern);
		List<File> accepted = filter.filterFiles(files);
		assertThat(accepted.size()).isEqualTo(1);
	}

	@Test
	public void noMatchWithSingleFile() {
		File[] files = new File[] {new File("/some/path/Test.txt")};
		Pattern pattern = Pattern.compile("[a-z]+\\.txt");
		RegexPatternFileListFilter filter = new RegexPatternFileListFilter(pattern);
		List<File> accepted = filter.filterFiles(files);
		assertThat(accepted.size()).isEqualTo(0);
	}

	@Test
	public void matchSubset() {
		File[] files = new File[] {
				new File("/some/path/foo.txt"),
				new File("/some/path/foo.not"),
				new File("/some/path/bar.txt"),
				new File("/some/path/bar.not")};
		Pattern pattern = Pattern.compile("[a-z]+\\.txt");
		RegexPatternFileListFilter filter = new RegexPatternFileListFilter(pattern);
		List<File> accepted = filter.filterFiles(files);
		assertThat(accepted.size()).isEqualTo(2);
		assertThat(accepted.contains(new File("/some/path/foo.txt"))).isTrue();
		assertThat(accepted.contains(new File("/some/path/bar.txt"))).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternEditorInContext() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"patternMatchingFileListFilterTests.xml", this.getClass());
		FileListFilter<File> filter = (FileListFilter<File>) context.getBean("filter");
		File[] files = new File[] {new File("/some/path/foo.txt")};
		List<File> accepted = filter.filterFiles(files);
		assertThat(accepted.size()).isEqualTo(1);
		context.close();
	}

	@Test(expected = BeanCreationException.class)
	public void invalidPatternSyntax() throws Throwable {
		new ClassPathXmlApplicationContext("invalidPatternMatchingFileListFilterTests.xml", this.getClass()).close();
	}

}
