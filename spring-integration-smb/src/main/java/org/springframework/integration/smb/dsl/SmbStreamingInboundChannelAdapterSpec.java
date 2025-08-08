/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.dsl;

import java.util.Comparator;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.dsl.RemoteFileStreamingInboundChannelAdapterSpec;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.smb.filters.SmbPersistentAcceptOnceFileListFilter;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.inbound.SmbStreamingMessageSource;
import org.springframework.lang.Nullable;

/**
 * A {@link RemoteFileStreamingInboundChannelAdapterSpec} for a {@link SmbStreamingMessageSource}.
 *
 * @author Gregory Bragg
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class SmbStreamingInboundChannelAdapterSpec
		extends RemoteFileStreamingInboundChannelAdapterSpec<SmbFile, SmbStreamingInboundChannelAdapterSpec,
		SmbStreamingMessageSource> {

	protected SmbStreamingInboundChannelAdapterSpec(RemoteFileTemplate<SmbFile> remoteFileTemplate,
			@Nullable Comparator<SmbFile> comparator) {

		this.target = new SmbStreamingMessageSource(remoteFileTemplate, comparator);
	}

	/**
	 * Specify a simple pattern to match remote files (e.g. '*.txt').
	 * @param pattern the pattern.
	 * @see SmbSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SmbStreamingInboundChannelAdapterSpec patternFilter(String pattern) {
		return filter(composeFilters(new SmbSimplePatternFileListFilter(pattern)));
	}

	/**
	 * Specify a regular expression to match remote files (e.g. '[0-9].*.txt').
	 * @param regex the expression.
	 * @see SmbRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SmbStreamingInboundChannelAdapterSpec regexFilter(String regex) {
		return filter(composeFilters(new SmbRegexPatternFileListFilter(regex)));
	}

	private CompositeFileListFilter<SmbFile> composeFilters(FileListFilter<SmbFile> fileListFilter) {
		CompositeFileListFilter<SmbFile> compositeFileListFilter = new CompositeFileListFilter<>();
		compositeFileListFilter.addFilters(fileListFilter,
				new SmbPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "smbStreamingMessageSource"));
		return compositeFileListFilter;
	}

}
