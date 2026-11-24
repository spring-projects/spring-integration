/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
