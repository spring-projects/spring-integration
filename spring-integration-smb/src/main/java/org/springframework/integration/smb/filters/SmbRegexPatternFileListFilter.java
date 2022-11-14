/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.UncheckedIOException;
import java.util.regex.Pattern;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

/**
 * Implementation of {@link AbstractRegexPatternFileListFilter} for SMB.
 *
 * @author Markus Spann
 * @author Prafull Kumar Soni
 *
 * @since 6.0
 */
public class SmbRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<SmbFile> {

	public SmbRegexPatternFileListFilter(String pattern) {
		this(Pattern.compile(pattern));
	}

	public SmbRegexPatternFileListFilter(Pattern pattern) {
		super(pattern);
	}

	/**
	 * Gets the specified SMB file's name.
	 * @param file SMB file object
	 * @return file name
	 * @see AbstractRegexPatternFileListFilter#getFilename(java.lang.Object)
	 */
	@Override
	protected String getFilename(SmbFile file) {
		return (file != null ? file.getName() : null);
	}

	@Override
	protected boolean isDirectory(SmbFile file) {
		try {
			return file.isDirectory();
		}
		catch (SmbException e) {
			throw new UncheckedIOException(e);
		}
	}

}
