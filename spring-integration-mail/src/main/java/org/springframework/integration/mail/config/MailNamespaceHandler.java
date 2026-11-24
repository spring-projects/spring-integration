/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mail.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the 'mail' namespace.
 *
 * @author Mark Fisher
 */
public class MailNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("outbound-channel-adapter", new MailOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("inbound-channel-adapter", new MailInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("imap-idle-channel-adapter", new ImapIdleChannelAdapterParser());
		this.registerBeanDefinitionParser("header-enricher", new MailHeaderEnricherParser());
		this.registerBeanDefinitionParser("mail-to-string-transformer", new MailToStringTransformerParser());
	}

}
