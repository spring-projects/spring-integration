package org.springframework.integration.file;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;

/**
 * @author Aaron Grant
 */
public class ChainFileListFilterIntegrationTests {

	private class MockOldFile extends File {
		private static final long serialVersionUID = 1L;

		public MockOldFile(String pathname) {
			super(pathname);
		}

		@Override
		public long lastModified() {
			return 1;
		}
	}

	private File[] noFiles = new File[0];
	private File[] oneFile = new File[] { new MockOldFile("file.txt") };

	@Test
	public void singleModifiedFilterNoFiles() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(noFiles);
			assertEquals(0, result.size());
		}
	}

	@Test
	public void singlePatternFilter() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new SimplePatternFileListFilter("*.txt"));
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

	@Test
	public void singleModifiedFilter() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

	@Test
	public void patternThenModifiedFilters() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new SimplePatternFileListFilter("*.txt"));
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

	@Test
	public void modifiedThenPatternFilters() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			chain.addFilter(new SimplePatternFileListFilter("*.txt"));
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

}
