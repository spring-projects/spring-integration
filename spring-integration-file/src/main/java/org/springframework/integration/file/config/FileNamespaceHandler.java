/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
