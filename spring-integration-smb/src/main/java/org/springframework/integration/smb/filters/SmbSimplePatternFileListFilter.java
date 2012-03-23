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

import jcifs.smb.SmbFile;

import org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter;

/**
 * Implementation of {@link AbstractSimplePatternFileListFilter} for SMB.
 * 
 * @author Markus Spann
 */
public class SmbSimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<SmbFile> {

	private final String toString;

	public SmbSimplePatternFileListFilter(String _pathPattern) {
		super(_pathPattern);
		toString = getClass().getName() + "[pattern='" + _pathPattern + "']";
	}

	/**
	 * Gets the specified SMB file's name.
	 * @param _file SMB file object
	 * @return file name
	 * @see org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter#getFilename(java.lang.Object)
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
