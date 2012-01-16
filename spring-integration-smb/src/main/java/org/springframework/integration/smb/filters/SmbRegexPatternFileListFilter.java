/**
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.smb.filters;

import java.util.regex.Pattern;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

/**
 * Implementation of {@link AbstractRegexPatternFileListFilter} for SMB.
 * 
 * @author Markus Spann
 * @since 2.1.1
 */
public class SmbRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<SmbFile> {

	private final String toString;

	public SmbRegexPatternFileListFilter(String _pattern) {
		this(Pattern.compile(_pattern));
	}

	public SmbRegexPatternFileListFilter(Pattern _pattern) {
		super(_pattern);
		toString = getClass().getName() + "[pattern='" + _pattern + "']";
	}

	/**
	 * Gets the specified SMB file's name.
	 * @param _file SMB file object
	 * @return file name
	 * @see org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter#getFilename(java.lang.Object)
	 */
	@Override
	protected String getFilename(SmbFile _file) {
		return (_file != null) ? _file.getName() : null;
	}

	@Override
	public String toString() {
		return toString;
	}

}
