/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.file.support;

import java.lang.reflect.Array;
import java.nio.file.FileSystems;
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

	private FileUtils() {
	}

}
