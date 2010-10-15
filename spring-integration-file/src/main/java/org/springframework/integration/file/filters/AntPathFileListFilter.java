package org.springframework.integration.file.filters;

import org.springframework.integration.file.entries.AbstractEntryListFilter;
import org.springframework.util.AntPathMatcher;

import java.io.File;

/**
 * @author Iwein Fuld
 */
public class AntPathFileListFilter extends AbstractEntryListFilter<File> {

	private final AntPathMatcher matcher = new AntPathMatcher();
	private final String path;

	public AntPathFileListFilter(String path) {
		this.path = path;
	}

	@Override
	public boolean accept(File file) {
		return matcher.match( path, file.getPath());
	}
}
