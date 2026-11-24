/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.filters;

/**
 * Simple implementation of {@link FileListFilter} that always returns true.
 * Suitable as a default.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class AcceptAllFileListFilter<F> extends AbstractFileListFilter<F> {

	@Override
	public boolean accept(F file) {
		return true;
	}

}
