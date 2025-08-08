/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
