/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.jdbc.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the integration JDBC schema.
 *
 * @author Jonas Partner
 * @author Dave Syer
 * @author Gunnar Hillert
 *
 * @since 2.0
 */
public class JdbcNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new JdbcPollingChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new JdbcMessageHandlerParser());
		registerBeanDefinitionParser("outbound-gateway", new JdbcOutboundGatewayParser());
		registerBeanDefinitionParser("message-store", new JdbcMessageStoreParser());
		registerBeanDefinitionParser("stored-proc-outbound-channel-adapter", new StoredProcMessageHandlerParser());
		registerBeanDefinitionParser("stored-proc-inbound-channel-adapter", new StoredProcPollingChannelAdapterParser());
		registerBeanDefinitionParser("stored-proc-outbound-gateway", new StoredProcOutboundGatewayParser());
	}

}
