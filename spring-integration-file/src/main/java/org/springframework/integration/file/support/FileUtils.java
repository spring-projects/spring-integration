/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.integration.file.support;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Utilities for operations on Files.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public final class FileUtils {

	public static final boolean IS_POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

	/**
	 * Remove entries from the array if the predicate returns true for an element.
	 * @param fileArray the array.
	 * @param predicate the predicate.
	 * @param comparator an optional comparator to sort the results.
	 * @param <F> the file type.
	 * @return the array of remaining elements.
	 * @since 5.0.7
	 */
	@SuppressWarnings("unchecked")
	public static <F> F[] purgeUnwantedElements(F[] fileArray, Predicate<? extends F> predicate,
			@Nullable Comparator<? extends F> comparator) {

		if (ObjectUtils.isEmpty(fileArray)) {
			return fileArray;
		}
		else {
			if (comparator == null) {
				return Arrays.stream(fileArray)
						.filter((Predicate<? super F>) predicate.negate())
						.toArray(size -> (F[]) Array.newInstance(fileArray[0].getClass(), size));
			}
			else {
				return Arrays.stream(fileArray)
						.filter((Predicate<? super F>) predicate.negate())
						.sorted((Comparator<? super F>) comparator)
						.toArray(size -> (F[]) Array.newInstance(fileArray[0].getClass(), size));
			}
		}
	}

	/**
	 * Create a new file instance representing the specified file within the given base directory,
	 * ensuring that the resulting file remains within the intended target directory.
	 * Prevent Path Traversal vulnerabilities by explicitly rejecting file names that are absolute
	 * paths or that resolve to canonical paths outside the provided base directory.
	 * @param directory The base directory where the file is intended to be placed.
	 * @param fileName  The name of the file or the relative path to be resolved against the base directory.
	 * @return A {@link File} object representing the securely resolved destination path.
	 * @throws InvalidPathException If the {@code fileName} is an absolute path or attempts to traverse
	 * outside the {@code directory}.
	 * @throws UncheckedIOException If an I/O error occurs while resolving the canonical paths.
	 * @since 5.5.21
	 */
	public static File newFileInDirectoryIfValid(File directory, String fileName) {
		if (new File(fileName).isAbsolute()) {
			throw new InvalidPathException(fileName,
					"The file is trying to leave the target output directory of " + directory);
		}
		try {
			File file = new File(directory.getCanonicalFile(), fileName);
			if (!file.getCanonicalPath().equals(file.getAbsolutePath())) {
				throw new InvalidPathException(fileName,
						"The file is trying to leave the target output directory of " + directory);
			}
			return file;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private FileUtils() {
	}

}
