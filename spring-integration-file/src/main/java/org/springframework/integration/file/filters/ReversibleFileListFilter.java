/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.filters;

import java.util.List;

/**
 *
 * A {@link FileListFilter} that allows the caller to reverse (roll back) state
 * changes.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Gary Russell
 *
 * @since 4.0.4
 */
public interface ReversibleFileListFilter<F> extends FileListFilter<F> {

	/**
	 * Indicate that not all files previously passed by this filter (in {@link #filterFiles(Object[])}
	 * have been processed; the file must be in the list of files; it, and all files after it, will
	 * be considered to have not been processed and will be considered next time.
	 * @param file the file which failed.
	 * @param files the list of files that were returned by {@link #filterFiles(Object[])}.
	 */
	void rollback(F file, List<F> files);

}
