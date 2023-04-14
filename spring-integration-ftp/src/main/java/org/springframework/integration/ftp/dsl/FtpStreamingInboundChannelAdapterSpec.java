/*
 * Copyright 2014-2023 the original author or authors.
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

package org.springframework.integration.ftp.dsl;

import java.util.Comparator;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.dsl.RemoteFileStreamingInboundChannelAdapterSpec;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpStreamingMessageSource;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.lang.Nullable;

/**
 * A {@link RemoteFileStreamingInboundChannelAdapterSpec} for a {@link FtpStreamingMessageSource}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class FtpStreamingInboundChannelAdapterSpec
		extends RemoteFileStreamingInboundChannelAdapterSpec<FTPFile, FtpStreamingInboundChannelAdapterSpec,
		FtpStreamingMessageSource> {

	protected FtpStreamingInboundChannelAdapterSpec(RemoteFileTemplate<FTPFile> remoteFileTemplate,
			@Nullable Comparator<FTPFile> comparator) {

		this.target = new FtpStreamingMessageSource(remoteFileTemplate, comparator);
	}

	/**
	 * Specify a simple pattern to match remote files (e.g. '*.txt').
	 * @param pattern the pattern.
	 * @see FtpSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public FtpStreamingInboundChannelAdapterSpec patternFilter(String pattern) {
		return filter(composeFilters(new FtpSimplePatternFileListFilter(pattern)));
	}

	/**
	 * Specify a regular expression to match remote files (e.g. '[0-9].*.txt').
	 * @param regex the expression.
	 * @see FtpRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public FtpStreamingInboundChannelAdapterSpec regexFilter(String regex) {
		return filter(composeFilters(new FtpRegexPatternFileListFilter(regex)));
	}

	private CompositeFileListFilter<FTPFile> composeFilters(FileListFilter<FTPFile> fileListFilter) {
		CompositeFileListFilter<FTPFile> compositeFileListFilter = new CompositeFileListFilter<>();
		compositeFileListFilter.addFilters(fileListFilter,
				new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "ftpStreamingMessageSource"));
		return compositeFileListFilter;
	}

}
