/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ftp.config;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.config.AbstractRemoteFileStreamingInboundChannelAdapterParser;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.inbound.FtpStreamingMessageSource;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class FtpStreamingInboundChannelAdapterParser extends AbstractRemoteFileStreamingInboundChannelAdapterParser {

	@Override
	protected Class<? extends RemoteFileOperations<?>> getTemplateClass() {
		return FtpRemoteFileTemplate.class;
	}

	@Override
	protected Class<? extends MessageSource<?>> getMessageSourceClass() {
		return FtpStreamingMessageSource.class;
	}

	@Override
	protected Class<? extends FileListFilter<?>> getSimplePatternFileListFilterClass() {
		return FtpSimplePatternFileListFilter.class;
	}

	@Override
	protected Class<? extends FileListFilter<?>> getRegexPatternFileListFilterClass() {
		return FtpRegexPatternFileListFilter.class;
	}

	@Override
	protected Class<? extends AbstractPersistentAcceptOnceFileListFilter<?>> getPersistentAcceptOnceFileListFilterClass() {
		return FtpPersistentAcceptOnceFileListFilter.class;
	}

}
