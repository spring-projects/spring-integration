/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the Apache Kafka namespace.
 *
 * @author Soby Chacko
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("outbound-channel-adapter", new KafkaOutboundChannelAdapterParser());
		registerBeanDefinitionParser("message-driven-channel-adapter", new KafkaMessageDrivenChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new KafkaOutboundGatewayParser());
		registerBeanDefinitionParser("inbound-gateway", new KafkaInboundGatewayParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new KafkaInboundChannelAdapterParser());
		KafkaChannelParser channelParser = new KafkaChannelParser();
		registerBeanDefinitionParser("channel", channelParser);
		registerBeanDefinitionParser("pollable-channel", channelParser);
		registerBeanDefinitionParser("publish-subscribe-channel", channelParser);
	}

}
