/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.integration.config.annotation.AnnotationDrivenParser;
import org.springframework.integration.gateway.config.GatewayParser;

/**
 * Namespace handler for the integration namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class IntegrationNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser("message-bus", new MessageBusParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenParser());
		registerBeanDefinitionParser("channel", new PointToPointChannelParser());
		registerBeanDefinitionParser("thread-local-channel", new ThreadLocalChannelParser());
		registerBeanDefinitionParser("publish-subscribe-channel", new PublishSubscribeChannelParser());
		registerBeanDefinitionParser("service-activator", new ServiceActivatorParser());
		registerBeanDefinitionParser("channel-adapter", new ChannelAdapterParser());
		registerBeanDefinitionParser("gateway", new GatewayParser());
		registerBeanDefinitionParser("selector-chain", new SelectorChainParser());
		registerBeanDefinitionParser("router", new RouterParser());
		registerBeanDefinitionParser("splitter", new SplitterParser());
		registerBeanDefinitionParser("aggregator", new AggregatorParser());
		registerBeanDefinitionParser("resequencer", new ResequencerParser());
		registerBeanDefinitionParser("pool-executor", new PoolExecutorParser());
	}

}
