/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.hazelcast.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the Hazelcast schema.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastIntegrationNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new HazelcastEventDrivenInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new HazelcastOutboundChannelAdapterParser());
		registerBeanDefinitionParser("cq-inbound-channel-adapter", new HazelcastContinuousQueryInboundChannelAdapterParser());
		registerBeanDefinitionParser("ds-inbound-channel-adapter", new HazelcastDistributedSQLInboundChannelAdapterParser());
		registerBeanDefinitionParser("cm-inbound-channel-adapter", new HazelcastClusterMonitorInboundChannelAdapterParser());
	}

}
