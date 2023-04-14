/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.integration.smb.dsl;

import java.io.File;
import java.util.Comparator;

import jcifs.smb.SmbFile;

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
import org.springframework.lang.Nullable;

/**
 * A {@link RemoteFileInboundChannelAdapterSpec} for an {@link SmbInboundFileSynchronizingMessageSource}.
 *
 * @author Gregory Bragg
 * @author Artem Bilan
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
