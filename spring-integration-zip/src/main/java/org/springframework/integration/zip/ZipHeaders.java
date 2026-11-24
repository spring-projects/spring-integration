/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
