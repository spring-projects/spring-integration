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

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.integration.config.annotation.AnnotationDrivenParser;
import org.springframework.integration.gateway.config.GatewayParser;
import org.springframework.util.ClassUtils;

/**
 * Namespace handler for the integration namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class IntegrationNamespaceHandler extends NamespaceHandlerSupport {

	private static final String ADAPTER_PARSER_MAPPINGS_LOCATION = "META-INF/spring-integration.parsers";


	private final Log logger = LogFactory.getLog(this.getClass()); 


	public void init() {
		registerBeanDefinitionParser("message-bus", new MessageBusParser());
		registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenParser());
		registerBeanDefinitionParser("channel", new DefaultChannelParser());
		registerBeanDefinitionParser("queue-channel", new QueueChannelParser());
		registerBeanDefinitionParser("publish-subscribe-channel", new PublishSubscribeChannelParser());
		registerBeanDefinitionParser("direct-channel", new DirectChannelParser());
		registerBeanDefinitionParser("priority-channel", new PriorityChannelParser());
		registerBeanDefinitionParser("rendezvous-channel", new RendezvousChannelParser());
		registerBeanDefinitionParser("thread-local-channel", new ThreadLocalChannelParser());
		registerBeanDefinitionParser("service-activator", new ServiceActivatorParser());
		registerBeanDefinitionParser("channel-adapter", new ChannelAdapterParser());
		registerBeanDefinitionParser("gateway", new GatewayParser());
		registerBeanDefinitionParser("handler", new HandlerParser());
		registerBeanDefinitionParser("handler-chain", new HandlerParser());
		registerBeanDefinitionParser("selector-chain", new SelectorChainParser());
		registerBeanDefinitionParser("router", new RouterParser());
		registerBeanDefinitionParser("splitter", new SplitterParser());
		registerBeanDefinitionParser("aggregator", new AggregatorParser());
		registerBeanDefinitionParser("resequencer", new ResequencerParser());
		registerBeanDefinitionParser("pool-executor", new PoolExecutorParser());
		Map<String, Class<? extends BeanDefinitionParser>> parserMappings = this.loadAdapterParserMappings();
		try {
			for (Map.Entry<String, Class<? extends BeanDefinitionParser>> entry : parserMappings.entrySet()) {
				registerBeanDefinitionParser(entry.getKey(), (BeanDefinitionParser) entry.getValue().newInstance());
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to instantiate BeanDefinitionParser.", e);
		}
	}

	private Map<String, Class<? extends BeanDefinitionParser>> loadAdapterParserMappings() {
		Map<String, Class<? extends BeanDefinitionParser>> parserMappings =
				new HashMap<String, Class<? extends BeanDefinitionParser>>();
		ClassLoader classLoader = getClass().getClassLoader();
		try {
			Properties mappings =
					PropertiesLoaderUtils.loadAllProperties(ADAPTER_PARSER_MAPPINGS_LOCATION, classLoader);
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded parser mappings [" + mappings + "]");
			}
			Enumeration<?> propertyNames = mappings.propertyNames();
			while (propertyNames.hasMoreElements()) {
				String name = (String) propertyNames.nextElement();
				String classname = mappings.getProperty(name);
				Class<?> parserClass = ClassUtils.forName(classname, classLoader);
				if (!BeanDefinitionParser.class.isAssignableFrom(parserClass)) {
					throw new IllegalStateException("Expected class of type BeanDefinitionParser, but '" +
							name + "' was of type '" + parserClass.getSimpleName() + "'");
				}
				parserMappings.put(name, (Class<? extends BeanDefinitionParser>) parserClass);
			}
			return parserMappings;
		}
		catch (IOException e) {
			throw new IllegalStateException(
					"Unable to load BeanDefinitionParser mappings from location [" +
					ADAPTER_PARSER_MAPPINGS_LOCATION + "]. Root cause: " + e);
		}
		catch (ClassNotFoundException e) {
			throw new IllegalStateException("Failed to load BeanDefinitionParser.", e);
		}
	}

}
