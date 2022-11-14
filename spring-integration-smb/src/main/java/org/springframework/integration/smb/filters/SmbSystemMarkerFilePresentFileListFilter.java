/*
 * Copyright 2018-2022 the original author or authors.
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

package org.springframework.integration.smb.filters;

import java.util.Map;
import java.util.function.Function;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractMarkerFilePresentFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * Implementation of {@link AbstractMarkerFilePresentFileListFilter} for SMB.
 *
 * @author Prafull Kumar Soni
 *
 * @since 6.0
 */
public class SmbSystemMarkerFilePresentFileListFilter extends AbstractMarkerFilePresentFileListFilter<SmbFile> {

	public SmbSystemMarkerFilePresentFileListFilter(FileListFilter<SmbFile> filter) {
		super(filter);
	}

	public SmbSystemMarkerFilePresentFileListFilter(FileListFilter<SmbFile> filter, String suffix) {
		super(filter, suffix);
	}

	public SmbSystemMarkerFilePresentFileListFilter(FileListFilter<SmbFile> filter, Function<String, String> function) {
		super(filter, function);
	}

	public SmbSystemMarkerFilePresentFileListFilter(
			Map<FileListFilter<SmbFile>, Function<String, String>> filtersAndFunctions) {

		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(SmbFile file) {
		return file.getName();
	}

}
