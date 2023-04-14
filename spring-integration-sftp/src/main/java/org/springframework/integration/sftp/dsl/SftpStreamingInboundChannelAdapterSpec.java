/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.sftp.dsl;

import java.util.Comparator;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.dsl.RemoteFileStreamingInboundChannelAdapterSpec;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.lang.Nullable;

/**
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public class SftpStreamingInboundChannelAdapterSpec
		extends RemoteFileStreamingInboundChannelAdapterSpec<SftpClient.DirEntry,
		SftpStreamingInboundChannelAdapterSpec, SftpStreamingMessageSource> {

	protected SftpStreamingInboundChannelAdapterSpec(RemoteFileTemplate<SftpClient.DirEntry> remoteFileTemplate,
			@Nullable Comparator<SftpClient.DirEntry> comparator) {

		this.target = new SftpStreamingMessageSource(remoteFileTemplate, comparator);
	}

	/**
	 * Specify a simple pattern to match remote files (e.g. '*.txt').
	 * @param pattern the pattern.
	 * @see SftpSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SftpStreamingInboundChannelAdapterSpec patternFilter(String pattern) {
		return filter(composeFilters(new SftpSimplePatternFileListFilter(pattern)));
	}

	/**
	 * Specify a regular expression to match remote files (e.g. '[0-9].*.txt').
	 * @param regex the expression.
	 * @see SftpRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	@Override
	public SftpStreamingInboundChannelAdapterSpec regexFilter(String regex) {
		return filter(composeFilters(new SftpRegexPatternFileListFilter(regex)));
	}

	private CompositeFileListFilter<SftpClient.DirEntry> composeFilters(
			FileListFilter<SftpClient.DirEntry> fileListFilter) {

		CompositeFileListFilter<SftpClient.DirEntry> compositeFileListFilter = new CompositeFileListFilter<>();
		compositeFileListFilter.addFilters(fileListFilter,
				new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftpStreamingMessageSource"));
		return compositeFileListFilter;
	}

}
