/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.filters;

import java.io.File;

/**
 * {@link FileListFilter} implementation that ignores any hidden files. Uses
 * {@link File#isHidden()} to make that determination.
 *
 * @author Gunnar Hillert
 * @since 4.2
 */
public class IgnoreHiddenFileListFilter extends AbstractFileListFilter<File> {

	/**
	 * @return Returns {@code true} for any non-hidden files.
	 */
	@Override
	public boolean accept(File file) {
		return !file.isHidden();
	}

}
