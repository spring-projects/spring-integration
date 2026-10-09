/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The {@link FileListFilter} to accept only files which are recent according to provided {@code age}:
 * the {@code lastModified} of the file is more than the age in comparison with the current time.
 * In other words, accept those files which are not old enough yet.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 */
public abstract class AbstractRecentFileListFilter<F> implements FileListFilter<F> {

	protected static final long ONE_SECOND = 1000;

	private final Duration age;

	/**
	 * Construct an instance with the default age as 1 day.
	 */
	public AbstractRecentFileListFilter() {
		this(Duration.ofDays(1));
	}

	public AbstractRecentFileListFilter(Duration age) {
		this.age = age;
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return true;
	}

	@Override
	public List<F> filterFiles(F @Nullable [] files) {
		if (files != null) {
			List<F> list = new ArrayList<>(files.length);
			Instant now = Instant.now();
			for (F file : files) {
				if (!fileIsAged(file, now)) {
					list.add(file);
				}
			}
			return list;
		}

		return Collections.emptyList();
	}

	@Override
	public boolean accept(F file) {
		return !fileIsAged(file, Instant.now());
	}

	protected boolean fileIsAged(F file, Instant now) {
		return getLastModified(file).plus(this.age).isBefore(now);
	}

	protected abstract Instant getLastModified(F remoteFile);

}
