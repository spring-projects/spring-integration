/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's 'file' namespace.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class FileNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new FileInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new FileOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new FileOutboundGatewayParser());
		registerBeanDefinitionParser("file-to-string-transformer", new FileToStringTransformerParser());
		registerBeanDefinitionParser("file-to-bytes-transformer", new FileToByteArrayTransformerParser());
		registerBeanDefinitionParser("tail-inbound-channel-adapter", new FileTailInboundChannelAdapterParser());
		registerBeanDefinitionParser("splitter", new FileSplitterParser());
	}

}
