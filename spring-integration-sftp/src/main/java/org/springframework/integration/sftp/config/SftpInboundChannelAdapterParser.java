/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.config;

import org.springframework.integration.file.config.AbstractRemoteFileInboundChannelAdapterParser;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.synchronizer.InboundFileSynchronizer;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;

/**
 * Parser for 'sftp:inbound-channel-adapter'
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpInboundChannelAdapterParser extends AbstractRemoteFileInboundChannelAdapterParser {

	@Override
	protected String getMessageSourceClassname() {
		return SftpInboundFileSynchronizingMessageSource.class.getName();
	}

	@Override
	protected Class<? extends InboundFileSynchronizer> getInboundFileSynchronizerClass() {
		return SftpInboundFileSynchronizer.class;
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
