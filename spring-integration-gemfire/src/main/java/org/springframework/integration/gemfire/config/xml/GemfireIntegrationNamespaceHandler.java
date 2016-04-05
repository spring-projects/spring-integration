/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.gemfire.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author David Turanski
 * @since 2.1
 */
public class GemfireIntegrationNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new GemfireInboundChannelAdapterParser());
		registerBeanDefinitionParser("cq-inbound-channel-adapter", new GemfireCqInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new GemfireOutboundChannelAdapterParser());
	}

}
