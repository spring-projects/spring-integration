/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.smb.config;

import org.springframework.integration.file.config.RemoteFileOutboundChannelAdapterParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;

/**
 * The parser for {@code <Int-smb:outbound-channel-adapter>}.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class SmbOutboundChannelAdapterParser extends RemoteFileOutboundChannelAdapterParser {

	@Override
	protected Class<? extends RemoteFileOperations<?>> getTemplateClass() {
		return SmbRemoteFileTemplate.class;
	}

}
