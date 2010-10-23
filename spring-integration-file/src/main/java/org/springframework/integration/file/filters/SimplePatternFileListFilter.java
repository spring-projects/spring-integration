package org.springframework.integration.file.filters;

import org.springframework.integration.file.entries.AbstractEntryListFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;

/**
 * Filter that supports ant style path expressions, which are less powerful but more readable than regular expressions.
 * This filter only filters on the name of the file, the rest of the path is ignored.
 *
 * @author Iwein Fuld
 * @see org.springframework.util.AntPathMatcher
 * @see org.springframework.integration.file.filters.PatternMatchingFileListFilter
 * @since 2.0.0
 */
public class SimplePatternFileListFilter extends AbstractEntryListFilter<File> implements FileListFilter {

	private final AntPathMatcher matcher = new AntPathMatcher();
	private final String path;

	public SimplePatternFileListFilter(String path) {
		this.path =  path;
	}

	/**
	 * Accept the given file its name matches the pattern,
	 */
	@Override
	public boolean accept(File file) {
		return matcher.match(path, file.getName());
	}

	public List<File> filterFiles(File[] files) {
		Assert.notNull("'files' must not be null.");
		return this.filterEntries(files);
	}
}
