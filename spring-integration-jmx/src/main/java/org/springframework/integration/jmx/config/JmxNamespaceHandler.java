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

package org.springframework.integration.jmx.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>jmx</em> namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Stuart Williams
 * @since 2.0
 */
public class JmxNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("operation-invoking-channel-adapter", new OperationInvokingChannelAdapterParser());
		this.registerBeanDefinitionParser("operation-invoking-outbound-gateway", new OperationInvokingOutboundGatewayParser());
		this.registerBeanDefinitionParser("attribute-polling-channel-adapter", new AttributePollingChannelAdapterParser());
		this.registerBeanDefinitionParser("tree-polling-channel-adapter", new MBeanTreePollingChannelAdapterParser());
		this.registerBeanDefinitionParser("notification-listening-channel-adapter", new NotificationListeningChannelAdapterParser());
		this.registerBeanDefinitionParser("notification-publishing-channel-adapter", new NotificationPublishingChannelAdapterParser());
		this.registerBeanDefinitionParser("mbean-export", new MBeanExporterParser());
	}

}
