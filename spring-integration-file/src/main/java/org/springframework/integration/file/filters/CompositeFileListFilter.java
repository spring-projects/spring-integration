/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.filters;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple {@link FileListFilter} that predicates its matches against <b>all</b> of the
 * configured {@link FileListFilter}.
 * <p>
 * Note: when {@link #discardCallback} is provided, it is populated to all the
 * {@link DiscardAwareFileListFilter} delegates. In this case, since this filter
 * matches the files against all delegates, the {@link #discardCallback} may be
 * called several times for the same file.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Gary Russell
 * @author Artem Bilan
 */
public class CompositeFileListFilter<F>
		implements ReversibleFileListFilter<F>, ResettableFileListFilter<F>, DiscardAwareFileListFilter<F>, Closeable {

	protected final Set<FileListFilter<F>> fileFilters; // NOSONAR

	private Consumer<F> discardCallback;

	private boolean allSupportAccept = true;

	private boolean oneIsForRecursion;


	public CompositeFileListFilter() {
		this.fileFilters = new LinkedHashSet<>();
	}

	public CompositeFileListFilter(Collection<? extends FileListFilter<F>> fileFilters) {
		this.fileFilters = new LinkedHashSet<>(fileFilters);
		this.allSupportAccept = fileFilters.stream().allMatch(FileListFilter::supportsSingleFileFiltering);
		this.oneIsForRecursion = fileFilters.stream().anyMatch(FileListFilter::isForRecursion);
	}

	@Override
	public boolean isForRecursion() {
		return this.oneIsForRecursion;
	}

	@Override
	public void close() throws IOException {
		for (FileListFilter<F> filter : this.fileFilters) {
			if (filter instanceof Closeable) {
				((Closeable) filter).close();
			}
		}
	}

	public CompositeFileListFilter<F> addFilter(FileListFilter<F> filter) {
		return addFilters(Collections.singletonList(filter));
	}

	/**
	 * @param filters one or more new filters to add
	 * @return this CompositeFileFilter instance with the added filters
	 * @see #addFilters(Collection)
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final CompositeFileListFilter<F> addFilters(FileListFilter<F>... filters) {
		List<FileListFilter<F>> asList = Arrays.asList(filters);
		return addFilters(asList);
	}

	/**
	 * Add the new filters to this CompositeFileListFilter while maintaining the existing filters.
	 * @param filtersToAdd a list of filters to add
	 * @return this CompositeFileListFilter instance with the added filters
	 */
	public synchronized CompositeFileListFilter<F> addFilters(Collection<? extends FileListFilter<F>> filtersToAdd) {
		for (FileListFilter<F> elf : filtersToAdd) {
			if (elf instanceof DiscardAwareFileListFilter) {
				((DiscardAwareFileListFilter<F>) elf).addDiscardCallback(this.discardCallback);
			}
			if (elf instanceof InitializingBean) {
				try {
					((InitializingBean) elf).afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
			this.allSupportAccept = this.allSupportAccept && elf.supportsSingleFileFiltering();
			this.oneIsForRecursion |= elf.isForRecursion();
		}
		this.fileFilters.addAll(filtersToAdd);
		return this;
	}

	@Override
	public void addDiscardCallback(Consumer<F> discardCallbackToSet) {
		this.discardCallback = discardCallbackToSet;
		if (this.discardCallback != null) {
			this.fileFilters
					.stream()
					.filter(DiscardAwareFileListFilter.class::isInstance)
					.map(f -> (DiscardAwareFileListFilter<F>) f)
					.forEach(f -> f.addDiscardCallback(discardCallbackToSet));
		}
	}

	@Override
	public List<F> filterFiles(F[] files) {
		Assert.notNull(files, "'files' should not be null");
		List<F> results = new ArrayList<>(Arrays.asList(files));
		for (FileListFilter<F> fileFilter : this.fileFilters) {
			List<F> currentResults = fileFilter.filterFiles(files);
			results.retainAll(currentResults);
		}
		return results;
	}

	@Override
	public boolean accept(F file) {
		AtomicBoolean allAccept = new AtomicBoolean(true);
		// we can't use stream().allMatch() because we have to call all filters for this filter's contract
		this.fileFilters.forEach(f -> allAccept.compareAndSet(true, f.accept(file)));
		return allAccept.get();
	}

	@Override
	public boolean supportsSingleFileFiltering() {
		return this.allSupportAccept;
	}

	@Override
	public void rollback(F file, List<F> files) {
		for (FileListFilter<F> fileFilter : this.fileFilters) {
			if (fileFilter instanceof ReversibleFileListFilter) {
				((ReversibleFileListFilter<F>) fileFilter).rollback(file, files);
			}
		}
	}

	@Override
	public boolean remove(F f) {
		boolean removed = false;
		for (FileListFilter<F> fileFilter : this.fileFilters) {
			if (fileFilter instanceof ResettableFileListFilter) {
				((ResettableFileListFilter<F>) fileFilter).remove(f);
				removed = true;
			}
		}
		return removed;
	}

}
