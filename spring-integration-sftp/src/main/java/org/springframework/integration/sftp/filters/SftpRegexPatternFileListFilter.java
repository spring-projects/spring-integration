/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.sftp.filters;

import java.util.regex.Pattern;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

/**
 * Implementation of {@link AbstractRegexPatternFileListFilter} for SFTP.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<SftpClient.DirEntry> {

	public SftpRegexPatternFileListFilter(String pattern) {
		super(pattern);
	}

	public SftpRegexPatternFileListFilter(Pattern pattern) {
		super(pattern);
	}

	@Override
	protected String getFilename(SftpClient.DirEntry entry) {
		return (entry != null) ? entry.getFilename() : null;
	}

	@Override
	protected boolean isDirectory(SftpClient.DirEntry file) {
		return file.getAttributes().isDirectory();
	}

}
