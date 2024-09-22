/*
 * Copyright 2017-2024 the original author or authors.
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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A FileListFilter that only passes files matched by one or more {@link FileListFilter}
 * if a corresponding marker file is also present to indicate a file transfer is complete.
 *
 * Since they look at multiple files, they cannot be used for late filtering in the
 * streaming message source.
 *
 * @param <F> the target protocol file type.
 *
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @since 5.0
 *
 */
public abstract class AbstractMarkerFilePresentFileListFilter<F> implements FileListFilter<F> {

	private final Map<FileListFilter<F>, Function<String, String>> filtersAndFunctions = new HashMap<>();

	/**
	 * Construct an instance with a single {@link FileListFilter} and ".complete"
	 * will be appended to the name of a matched file when looking for the marker file.
	 * i.e. if a file {@code foo.txt} is matched by the filter this filter will only pass
	 * "foo.txt" if "foo.txt.complete" is present.
	 * @param filter the file name filter.
	 */
	public AbstractMarkerFilePresentFileListFilter(FileListFilter<F> filter) {
		this(filter, defaultFileNameFunction(".complete"));
	}

	/**
	 * Construct an instance with a single {@link FileListFilter} and a suffix
	 * that will will be appended to the name of a matched file when looking for the marker
	 * file. i.e. if a file {@code foo.txt} is matched by the filter and the suffix is
	 * ".complete", this filter will only pass "foo.txt" if "foo.txt.complete" is present.
	 * @param filter the file name filter.
	 * @param suffix the replacement suffix.
	 */
	public AbstractMarkerFilePresentFileListFilter(FileListFilter<F> filter, String suffix) {
		this(filter, defaultFileNameFunction(suffix));
	}

	/**
	 * Construct an instance with a single {@link FileListFilter} and a function
	 * that will be applied to the name of a matched file when looking for the marker
	 * file. The function returns the name of the marker file to match, or {@code null}
	 * for never match. If a file {@code foo.txt} is matched by the filter and the
	 * function returns "foo.txt.complete", this filter will only pass "foo.txt" if
	 * "foo.txt.complete" is present.
	 * @param filter the file name filter.
	 * @param function the function to create the marker file name from the file name.
	 */
	public AbstractMarkerFilePresentFileListFilter(FileListFilter<F> filter,
			Function<String, String> function) {
		this(Collections.singletonMap(filter, function));
	}

	/**
	 * Construct an instance with a map of {@link FileListFilter} and functions be
	 * applied to the name of a matched file when looking for the marker file. i.e. if a
	 * file {@code foo.txt} is matched by one of the filters and the corresponding
	 * function returns "foo.txt.complete", this filter will only pass "foo.txt" if
	 * "foo.txt.complete" is present. The function returns the name of the marker file to
	 * match, or {@code null} for never match. Due to type erasure, we cannot provide a
	 * constructor taking {@code Map<Filter, Function>}. For convenience, you can use
	 * {@link #defaultFileNameFunction(String)} to use the default function used by the
	 * {@link #AbstractMarkerFilePresentFileListFilter(FileListFilter, String)}
	 * constructor.
	 * @param filtersAndFunctions the filters and functions.
	 */
	public AbstractMarkerFilePresentFileListFilter(
			Map<FileListFilter<F>, Function<String, String>> filtersAndFunctions) {
		this.filtersAndFunctions.putAll(filtersAndFunctions);
	}

	/**
	 * The default function used to create the file name for the corresponding marker file.
	 * Appends a suffix to the file name.
	 * @param suffix the suffix to append.
	 * @return the function.
	 */
	public static Function<String, String> defaultFileNameFunction(final String suffix) {
		return s -> s + suffix;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<F> filterFiles(F[] files) {
		if (files.length < 2) {
			return Collections.emptyList();
		}
		final Set<String> candidates = Arrays.stream(files)
				.map(this::getFilename)
				.collect(Collectors.toSet());
		List<F> results = new ArrayList<>();
		for (F file : files) {
			boolean anyMatch = this.filtersAndFunctions.entrySet().stream().anyMatch(entry -> {
				F[] fileToCheck = (F[]) Array.newInstance(file.getClass(), 1);
				fileToCheck[0] = file;
				if (!entry.getKey().filterFiles(fileToCheck).isEmpty()) {
					String markerName = entry.getValue().apply(getFilename(file));
					return markerName != null && candidates.contains(markerName);
				}
				return false;
			});
			if (anyMatch) {
				results.add(file);
			}
		}
		return results;
	}

	/**
	 * Return the name of the file represented by this F.
	 * @param file the file.
	 * @return the name.
	 */
	protected abstract String getFilename(F file);

}
