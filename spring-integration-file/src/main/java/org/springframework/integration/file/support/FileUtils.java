/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.support;

import java.lang.reflect.Array;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utilities for operations on Files.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public final class FileUtils {

	public static final boolean IS_POSIX = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");

	/**
	 * Remove entries from the array if the predicate returns true for an element.
	 * @param fileArray the array.
	 * @param predicate the predicate.
	 * @param <F> the file type.
	 * @return the array (modified or not), or null if all elements are removed.
	 * @since 5.0.7
	 */
	public @Nullable static <F> F[] purgeUnwantedElements(F[] fileArray, Predicate<F> predicate) {
		List<F> files = new LinkedList<>(Arrays.asList(fileArray));
		Iterator<F> iterator = files.iterator();
		boolean removed = false;
		while (iterator.hasNext()) {
			F next = iterator.next();
			if (predicate.test(next)) {
				iterator.remove();
				removed = true;
			}
		}
		if (!removed) {
			return fileArray;
		}
		else if (files.size() == 0) {
			return null;
		}
		else {
			return toGenericArray(files);
		}
	}

	/**
	 * Create an array of generic type from a list. Must have at least one entry.
	 * @param files the list.
	 * @param <F> the file type.
	 * @return the array.
	 * @since 5.0.7
	 */
	public static <F> F[] toGenericArray(List<F> files) {
		Assert.isTrue(files.size() > 0, "file list must have at least one entry");
		@SuppressWarnings("unchecked")
		F[] array = (F[]) Array.newInstance(files.get(0).getClass(), files.size());
		for (int i = 0; i < files.size(); i++) {
			array[i] = files.get(i);
		}
		return array;
	}

	private FileUtils() {
		super();
	}

}
