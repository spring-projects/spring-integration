/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.syslog.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namspace handler for spring-integration-syslog.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SyslogNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("inbound-channel-adapter", new SyslogInboundChannelAdapterParser());
	}

}
