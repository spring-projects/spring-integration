/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.sftp.dsl;

import java.io.File;
import java.util.Comparator;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.dsl.RemoteFileInboundChannelAdapterSpec;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.lang.Nullable;

/**
 * A {@link RemoteFileInboundChannelAdapterSpec} for an {@link SftpInboundFileSynchronizingMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class SftpInboundChannelAdapterSpec
		extends RemoteFileInboundChannelAdapterSpec<SftpClient.DirEntry, SftpInboundChannelAdapterSpec,
		SftpInboundFileSynchronizingMessageSource> {

	protected SftpInboundChannelAdapterSpec(SessionFactory<SftpClient.DirEntry> sessionFactory,
			@Nullable Comparator<File> comparator) {

		super(new SftpInboundFileSynchronizer(sessionFactory));
		this.target = new SftpInboundFileSynchronizingMessageSource(this.synchronizer, comparator);
	}

	/**
	 * @param pattern the Ant style pattern filter to use.
	 * @see SftpSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SftpInboundChannelAdapterSpec patternFilter(String pattern) {
		return filter(composeFilters(new SftpSimplePatternFileListFilter(pattern)));
	}

	/**
	 * @param regex the RegExp pattern to use.
	 * @see SftpRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SftpInboundChannelAdapterSpec regexFilter(String regex) {
		return filter(composeFilters(new SftpRegexPatternFileListFilter(regex)));
	}

	private CompositeFileListFilter<SftpClient.DirEntry> composeFilters(FileListFilter<SftpClient.DirEntry>
			fileListFilter) {

		CompositeFileListFilter<SftpClient.DirEntry> compositeFileListFilter = new CompositeFileListFilter<>();
		compositeFileListFilter.addFilters(fileListFilter,
				new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftpMessageSource"));
		return compositeFileListFilter;
	}

}
