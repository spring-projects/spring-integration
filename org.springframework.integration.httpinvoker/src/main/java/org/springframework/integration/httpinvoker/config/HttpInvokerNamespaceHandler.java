/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.httpinvoker.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>httpinvoker</em> namespace.
 * 
 * @author Mark Fisher
 * 
 * @deprecated as of 2.0.x. We recommend using the REST-based HTTP adapters instead.
 */
@Deprecated
public class HttpInvokerNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("inbound-gateway", new HttpInvokerInboundGatewayParser());
		this.registerBeanDefinitionParser("outbound-gateway", new HttpInvokerOutboundGatewayParser());
	}

}
