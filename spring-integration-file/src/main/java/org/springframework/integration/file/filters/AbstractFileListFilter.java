/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.util.ArrayList;
import java.util.List;

/**
 * A convenience base class for any {@link FileListFilter} whose criteria can be
 * evaluated against each File in isolation. If the entire List of files is
 * required for evaluation, implement the FileListFilter interface directly.
 *
 * @param <F> the target protocol file type.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 */
public abstract class AbstractFileListFilter<F> implements FileListFilter<F> {

	@Override
	public final List<F> filterFiles(F[] files) {
		List<F> accepted = new ArrayList<F>();
		if (files != null) {
			for (F file : files) {
				if (this.accept(file)) {
					accepted.add(file);
				}
			}
		}
		return accepted;
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}

	/**
	 * Subclasses must implement this method.
	 * @param file The file.
	 * @return true if the file passes the filter.
	 */
	@Override
	public abstract boolean accept(F file);

}
