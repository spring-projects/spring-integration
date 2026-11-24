/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.sftp.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Provides namespace support for using SFTP.
 * This is largely based on the FTP support by Iwein Fuld.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg ZHurakousky
 * @since 2.0
 */
public class SftpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new SftpInboundChannelAdapterParser());
		registerBeanDefinitionParser("inbound-streaming-channel-adapter", new SftpStreamingInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new SftpOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new SftpOutboundGatewayParser());
	}

}
