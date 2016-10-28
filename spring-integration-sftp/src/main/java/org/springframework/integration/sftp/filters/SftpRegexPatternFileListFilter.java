/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.sftp.filters;

import java.util.regex.Pattern;

import org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/**
 * Implementation of {@link AbstractRegexPatternFileListFilter} for SFTP.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class SftpRegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<ChannelSftp.LsEntry> {

	public SftpRegexPatternFileListFilter(String pattern) {
		super(pattern);
	}

	public SftpRegexPatternFileListFilter(Pattern pattern) {
		super(pattern);
	}


	@Override
	protected String getFilename(LsEntry entry) {
		return (entry != null) ? entry.getFilename() : null;
	}

	@Override
	protected boolean isDirectory(LsEntry file) {
		return file.getAttrs().isDir();
	}

}
