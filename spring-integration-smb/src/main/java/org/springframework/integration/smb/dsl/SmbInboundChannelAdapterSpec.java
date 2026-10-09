/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.dsl;

import java.io.File;
import java.util.Comparator;

import org.codelibs.jcifs.smb.impl.SmbFile;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.dsl.RemoteFileInboundChannelAdapterSpec;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.smb.filters.SmbPersistentAcceptOnceFileListFilter;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizer;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizingMessageSource;

/**
 * A {@link RemoteFileInboundChannelAdapterSpec} for an {@link SmbInboundFileSynchronizingMessageSource}.
 *
 * @author Gregory Bragg
 * @author Artem Bilan
 * @author Daniel Frey
 *
 * @since 6.0
 */
public class SmbInboundChannelAdapterSpec
		extends RemoteFileInboundChannelAdapterSpec<SmbFile, SmbInboundChannelAdapterSpec,
		SmbInboundFileSynchronizingMessageSource> {

	protected SmbInboundChannelAdapterSpec(SessionFactory<SmbFile> sessionFactory,
			@Nullable Comparator<File> comparator) {

		super(new SmbInboundFileSynchronizer(sessionFactory));
		this.target = new SmbInboundFileSynchronizingMessageSource(this.synchronizer, comparator);
	}

	/**
	 * Specify a simple pattern to match remote files.
	 * @param pattern the pattern.
	 * @see SmbSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SmbInboundChannelAdapterSpec patternFilter(String pattern) {
		return filter(composeFilters(new SmbSimplePatternFileListFilter(pattern)));
	}

	/**
	 * Specify a regular expression to match remote files.
	 * @param regex the expression.
	 * @see SmbRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SmbInboundChannelAdapterSpec regexFilter(String regex) {
		return filter(composeFilters(new SmbRegexPatternFileListFilter(regex)));
	}

	private CompositeFileListFilter<SmbFile> composeFilters(FileListFilter<SmbFile> fileListFilter) {
		CompositeFileListFilter<SmbFile> compositeFileListFilter = new CompositeFileListFilter<>();
		compositeFileListFilter.addFilters(fileListFilter,
				new SmbPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "smbMessageSource"));
		return compositeFileListFilter;
	}

}
