/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zip;

/**
 * Zip adapter specific message headers.
 *
 * @author Gunnar Hillert
 *
 * @since 6.1
 */
public abstract class ZipHeaders {

	public static final String PREFIX = "zip_";

	public static final String ZIP_ENTRY_FILE_NAME = PREFIX + "entryFilename";

	public static final String ZIP_ENTRY_PATH = PREFIX + "entryPath";

	public static final String ZIP_ENTRY_LAST_MODIFIED_DATE = PREFIX + "entryLastModifiedDate";

}
