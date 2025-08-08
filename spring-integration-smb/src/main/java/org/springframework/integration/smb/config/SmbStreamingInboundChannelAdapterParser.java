/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.config;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.config.AbstractRemoteFileStreamingInboundChannelAdapterParser;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.smb.filters.SmbPersistentAcceptOnceFileListFilter;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.inbound.SmbStreamingMessageSource;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;

/**
 * Parser for the SMB 'inbound-streaming-channel-adapter' element.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbStreamingInboundChannelAdapterParser extends AbstractRemoteFileStreamingInboundChannelAdapterParser {

	@Override
	protected Class<? extends RemoteFileOperations<?>> getTemplateClass() {
		return SmbRemoteFileTemplate.class;
	}

	@Override
	protected Class<? extends MessageSource<?>> getMessageSourceClass() {
		return SmbStreamingMessageSource.class;
	}

	@Override
	protected Class<? extends FileListFilter<?>> getSimplePatternFileListFilterClass() {
		return SmbSimplePatternFileListFilter.class;
	}

	@Override
	protected Class<? extends FileListFilter<?>> getRegexPatternFileListFilterClass() {
		return SmbRegexPatternFileListFilter.class;
	}

	@Override
	protected Class<? extends AbstractPersistentAcceptOnceFileListFilter<?>> getPersistentAcceptOnceFileListFilterClass() {
		return SmbPersistentAcceptOnceFileListFilter.class;
	}

}
