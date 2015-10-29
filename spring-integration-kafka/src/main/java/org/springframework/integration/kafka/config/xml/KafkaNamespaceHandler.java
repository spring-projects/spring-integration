/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the Kafka namespace
 *
 * @author Soby Chacko
 * @since 0.5
 *
 */
public class KafkaNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	@SuppressWarnings("deprecation")
	public void init() {
		registerBeanDefinitionParser("zookeeper-connect", new ZookeeperConnectParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new KafkaInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new KafkaOutboundChannelAdapterParser());
		registerBeanDefinitionParser("producer-context", new KafkaProducerContextParser());
		registerBeanDefinitionParser("consumer-context", new KafkaConsumerContextParser());
		registerBeanDefinitionParser("message-driven-channel-adapter", new KafkaMessageDrivenChannelAdapterParser());
	}

}
