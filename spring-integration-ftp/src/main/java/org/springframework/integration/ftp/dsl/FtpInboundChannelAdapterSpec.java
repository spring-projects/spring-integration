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

import java.io.File;
import java.util.Comparator;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.dsl.RemoteFileInboundChannelAdapterSpec;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundFileSynchronizingMessageSource;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.lang.Nullable;

/**
 * A {@link RemoteFileInboundChannelAdapterSpec} for an {@link FtpInboundFileSynchronizingMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class FtpInboundChannelAdapterSpec
		extends RemoteFileInboundChannelAdapterSpec<FTPFile, FtpInboundChannelAdapterSpec,
		FtpInboundFileSynchronizingMessageSource> {

	protected FtpInboundChannelAdapterSpec(SessionFactory<FTPFile> sessionFactory,
			@Nullable Comparator<File> comparator) {

		super(new FtpInboundFileSynchronizer(sessionFactory));
		this.target = new FtpInboundFileSynchronizingMessageSource(this.synchronizer, comparator);
	}

	/**
	 * Specify a simple pattern to match remote files.
	 * @param pattern the pattern.
	 * @see FtpSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public FtpInboundChannelAdapterSpec patternFilter(String pattern) {
		return filter(composeFilters(new FtpSimplePatternFileListFilter(pattern)));
	}

	/**
	 * Specify a regular expression to match remote files.
	 * @param regex the expression.
	 * @see FtpRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public FtpInboundChannelAdapterSpec regexFilter(String regex) {
		return filter(composeFilters(new FtpRegexPatternFileListFilter(regex)));
	}

	private CompositeFileListFilter<FTPFile> composeFilters(FileListFilter<FTPFile> fileListFilter) {
		CompositeFileListFilter<FTPFile> compositeFileListFilter = new CompositeFileListFilter<>();
		compositeFileListFilter.addFilters(fileListFilter,
				new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "ftpMessageSource"));
		return compositeFileListFilter;
	}

}
