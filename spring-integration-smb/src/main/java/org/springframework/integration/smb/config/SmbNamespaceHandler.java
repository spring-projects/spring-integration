/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.integration.smb.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Provides namespace support for using SMB.
 *
 * @author Markus Spann
 * @author Artem Bilan
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new SmbInboundChannelAdapterParser());
		registerBeanDefinitionParser("inbound-streaming-channel-adapter", new SmbStreamingInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new SmbOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new SmbOutboundGatewayParser());
	}

}
