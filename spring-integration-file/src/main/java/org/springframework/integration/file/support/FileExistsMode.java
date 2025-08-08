/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.support;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * When writing file, this enumeration indicates what action shall be taken in
 * case the destination file already exists.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.2
 *
 */
public enum FileExistsMode {

	/**
	 * Append data to any pre-existing files; close after each append.
	 */
	APPEND,

	/**
	 * Append data to any pre-existing files; do not flush/close after
	 * appending.
	 * @since 4.3
	 */
	APPEND_NO_FLUSH,

	/**
	 * Raise an exception in case the file to be written already exists.
	 */
	FAIL,

	/**
	 * If the file already exists, do nothing.
	 */
	IGNORE,

	/**
	 * If the file already exists, replace it.
	 */
	REPLACE,

	/**
	 * If the file already exists, replace it only if the last modified time
	 * is different. Only applies to local files.
	 * @since 5.0
	 */
	REPLACE_IF_MODIFIED;

	/**
	 * For a given non-null and not-empty input string, this method returns the
	 * corresponding {@link FileExistsMode}. If it cannot be determined, an
	 * {@link IllegalStateException} is thrown.
	 *
	 * @param fileExistsModeAsString Must neither be null nor empty
	 * @return the enum for the string value.
	 */
	public static FileExistsMode getForString(String fileExistsModeAsString) {

		Assert.hasText(fileExistsModeAsString, "'fileExistsModeAsString' must neither be null nor empty.");

		final FileExistsMode[] fileExistsModeValues = FileExistsMode.values();

		for (FileExistsMode fileExistsMode : fileExistsModeValues) {
			if (fileExistsModeAsString.equalsIgnoreCase(fileExistsMode.name())) {
				return fileExistsMode;
			}
		}

		throw new IllegalArgumentException("Invalid fileExistsMode '" + fileExistsModeAsString
				+ "'. The (case-insensitive) supported values are: "
				+ StringUtils.arrayToCommaDelimitedString(fileExistsModeValues));

	}

}
