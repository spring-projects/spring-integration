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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Namespace handler for the integration namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class IntegrationNamespaceHandler implements NamespaceHandler {

	private static final String DEFAULT_CONFIGURING_POSTPROCESSOR_SIMPLE_CLASS_NAME =
			"DefaultConfiguringBeanFactoryPostProcessor";

	private static final String DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME =
			IntegrationNamespaceUtils.BASE_PACKAGE + ".internal" + DEFAULT_CONFIGURING_POSTPROCESSOR_SIMPLE_CLASS_NAME;


	private final NamespaceHandlerSupport delegate = new NamespaceHandlerDelegate();


	public void init() {
		this.delegate.init();
	}

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		this.registerDefaultConfiguringBeanFactoryPostProcessorIfNecessary(parserContext);
		return this.delegate.parse(element, parserContext);
	}

	public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext) {
		return this.delegate.decorate(source, definition, parserContext);
	}

	private void registerDefaultConfiguringBeanFactoryPostProcessorIfNecessary(ParserContext parserContext) {
		if (!parserContext.getRegistry().isBeanNameInUse(DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME)) {
			BeanDefinitionBuilder postProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".config.xml." + DEFAULT_CONFIGURING_POSTPROCESSOR_SIMPLE_CLASS_NAME);
			BeanDefinitionHolder postProcessorHolder = new BeanDefinitionHolder(
					postProcessorBuilder.getBeanDefinition(), DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(postProcessorHolder, parserContext.getRegistry());
		}
	}


	private static class NamespaceHandlerDelegate extends NamespaceHandlerSupport {

		public void init() {
			registerBeanDefinitionParser("channel", new PointToPointChannelParser());
			registerBeanDefinitionParser("thread-local-channel", new ThreadLocalChannelParser());
			registerBeanDefinitionParser("publish-subscribe-channel", new PublishSubscribeChannelParser());
			registerBeanDefinitionParser("service-activator", new ServiceActivatorParser());
			registerBeanDefinitionParser("transformer", new TransformerParser());
			registerBeanDefinitionParser("filter", new FilterParser());
			registerBeanDefinitionParser("router", new RouterParser());
			registerBeanDefinitionParser("splitter", new SplitterParser());
			registerBeanDefinitionParser("aggregator", new AggregatorParser());
			registerBeanDefinitionParser("resequencer", new ResequencerParser());
			registerBeanDefinitionParser("header-enricher", new StandardHeaderEnricherParser());
			registerBeanDefinitionParser("object-to-string-transformer", new ObjectToStringTransformerParser());
			registerBeanDefinitionParser("payload-serializing-transformer", new PayloadSerializingTransformerParser());
			registerBeanDefinitionParser("payload-deserializing-transformer", new PayloadDeserializingTransformerParser());
			registerBeanDefinitionParser("inbound-channel-adapter", new MethodInvokingInboundChannelAdapterParser());
			registerBeanDefinitionParser("outbound-channel-adapter", new MethodInvokingOutboundChannelAdapterParser());
			registerBeanDefinitionParser("logging-channel-adapter", new LoggingChannelAdapterParser());
			registerBeanDefinitionParser("gateway", new GatewayParser());
			registerBeanDefinitionParser("bridge", new BridgeParser());
			registerBeanDefinitionParser("chain", new ChainParser());
			registerBeanDefinitionParser("selector-chain", new SelectorChainParser());
			registerBeanDefinitionParser("poller", new PollerParser());
			registerBeanDefinitionParser("annotation-config", new AnnotationConfigParser());
			registerBeanDefinitionParser("application-event-multicaster", new ApplicationEventMulticasterParser());
			registerBeanDefinitionParser("thread-pool-task-executor", new ThreadPoolTaskExecutorParser());
		}
	}

}
