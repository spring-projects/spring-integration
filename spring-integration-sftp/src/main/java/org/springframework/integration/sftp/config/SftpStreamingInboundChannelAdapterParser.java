/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.sftp.config;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.config.AbstractRemoteFileStreamingInboundChannelAdapterParser;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpStreamingMessageSource;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class SftpStreamingInboundChannelAdapterParser extends AbstractRemoteFileStreamingInboundChannelAdapterParser {

	@Override
	protected Class<? extends RemoteFileOperations<?>> getTemplateClass() {
		return SftpRemoteFileTemplate.class;
	}

	@Override
	protected Class<? extends MessageSource<?>> getMessageSourceClass() {
		return SftpStreamingMessageSource.class;
	}

	@Override
	protected Class<? extends FileListFilter<?>> getSimplePatternFileListFilterClass() {
		return SftpSimplePatternFileListFilter.class;
	}

	@Override
	protected Class<? extends FileListFilter<?>> getRegexPatternFileListFilterClass() {
		return SftpRegexPatternFileListFilter.class;
	}

	@Override
	protected Class<? extends AbstractPersistentAcceptOnceFileListFilter<?>> getPersistentAcceptOnceFileListFilterClass() {
		return SftpPersistentAcceptOnceFileListFilter.class;
	}

}
