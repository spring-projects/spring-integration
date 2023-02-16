/*
 * Copyright 2015-2023 the original author or authors.
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
