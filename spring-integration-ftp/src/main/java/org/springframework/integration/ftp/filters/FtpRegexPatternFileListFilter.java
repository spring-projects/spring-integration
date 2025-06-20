/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ftp.filters;

import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

/**
 * Implementation of {@link AbstractRegexPatternFileListFilter} for FTP.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class FtpRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<FTPFile> {

	public FtpRegexPatternFileListFilter(String pattern) {
		super(pattern);
	}

	public FtpRegexPatternFileListFilter(Pattern pattern) {
		super(pattern);
	}

	@Override
	protected String getFilename(FTPFile file) {
		return (file != null) ? file.getName() : null;
	}

	@Override
	protected boolean isDirectory(FTPFile file) {
		return file.isDirectory();
	}

}
