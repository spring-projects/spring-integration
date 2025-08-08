/*
 * Copyright © 2012 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.smb.config;

import org.springframework.integration.file.config.AbstractRemoteFileInboundChannelAdapterParser;
import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.synchronizer.InboundFileSynchronizer;
import org.springframework.integration.smb.filters.SmbPersistentAcceptOnceFileListFilter;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizer;
import org.springframework.integration.smb.inbound.SmbInboundFileSynchronizingMessageSource;

/**
 * Parser for the SMB 'inbound-channel-adapter' element.
 *
 * @author Markus Spann
 * @author Artem Bilan
 * @author Prafull Kumar Soni
 *
 * @since 6.0
 */
public class SmbInboundChannelAdapterParser extends AbstractRemoteFileInboundChannelAdapterParser {

	@Override
	protected String getMessageSourceClassname() {
		return SmbInboundFileSynchronizingMessageSource.class.getName();
	}

	@Override
	protected Class<? extends InboundFileSynchronizer> getInboundFileSynchronizerClass() {
		return SmbInboundFileSynchronizer.class;
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
