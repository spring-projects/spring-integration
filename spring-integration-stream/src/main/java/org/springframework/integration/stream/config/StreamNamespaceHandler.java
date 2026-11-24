/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.stream.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Mark Fisher
 */
public class StreamNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("stdin-channel-adapter", new ConsoleInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("stdout-channel-adapter", new ConsoleOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("stderr-channel-adapter", new ConsoleOutboundChannelAdapterParser());
	}

}
