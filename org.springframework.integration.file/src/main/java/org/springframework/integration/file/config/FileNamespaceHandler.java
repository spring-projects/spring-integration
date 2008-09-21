package org.springframework.integration.file.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class FileNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new FileInboundChannelAdapterParser());
	}

}
