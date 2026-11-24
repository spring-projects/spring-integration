/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.filters;

import java.io.File;

/**
 * Filter that supports ant style path expressions, which are less powerful but more readable than regular expressions.
 * This filter only filters on the name of the file, the rest of the path is ignored.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class SimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<File> {

	public SimplePatternFileListFilter(String path) {
		super(path);
	}

	@Override
	protected String getFilename(File file) {
		return file.getName();
	}

	@Override
	protected boolean isDirectory(File file) {
		return file.isDirectory();
	}

}
