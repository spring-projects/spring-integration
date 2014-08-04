/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		registerBeanDefinitionParser("outbound-channel-adapter", new SftpOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new SftpOutboundGatewayParser());
	}

}
