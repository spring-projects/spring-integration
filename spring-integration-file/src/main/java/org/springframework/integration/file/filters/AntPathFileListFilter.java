package org.springframework.integration.file.filters;

import org.springframework.integration.file.entries.AbstractEntryListFilter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;

/**
 * Filter that supports ant style path expressions, which are less powerful but more readable than regular expressions.
 *
 * @author Iwein Fuld
 * @see org.springframework.integration.file.filters.PatternMatchingFileListFilter
 * @since 2.0.0
 */
public class AntPathFileListFilter extends AbstractEntryListFilter<File> implements FileListFilter {

	private final AntPathMatcher matcher = new AntPathMatcher();
	private final String path;

	public AntPathFileListFilter(String path) {
		this.path = path;
	}

	@Override
	public boolean accept(File file) {
		return matcher.match(path, file.getPath());
	}

	public List<File> filterFiles(File[] files) {
		Assert.notNull("'files' must not be null.");
		return this.filterEntries(files);
	}
}
