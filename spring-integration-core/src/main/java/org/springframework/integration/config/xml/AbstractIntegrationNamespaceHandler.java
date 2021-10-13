/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.concurrent.atomic.AtomicBoolean;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ChannelInitializer;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;

/**
 * Base class for NamespaceHandlers that registers a BeanFactoryPostProcessor
 * for configuring default bean definitions.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractIntegrationNamespaceHandler extends NamespaceHandlerSupport {

	private final AtomicBoolean initialized = new AtomicBoolean();

	@Override
	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		if (!this.initialized.getAndSet(true)) {
			BeanDefinitionRegistry registry = parserContext.getRegistry();
			new IntegrationRegistrar().registerBeanDefinitions(null, registry);
			registerImplicitChannelCreator(registry);
		}
		return super.parse(element, parserContext);
	}

	/**
	 * This method will auto-register a ChannelInitializer which could also be overridden by the user
	 * by simply registering a ChannelInitializer {@code <bean>} with its {@code autoCreate} property
	 * set to false to suppress channel creation.
	 * It will also register a ChannelInitializer$AutoCreateCandidatesCollector
	 * which simply collects candidate channel names.
	 * @param registry The {@link BeanDefinitionRegistry} to register additional {@link BeanDefinition}s.
	 */
	private static void registerImplicitChannelCreator(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.CHANNEL_INITIALIZER_BEAN_NAME)) {
			String channelsAutoCreateExpression =
					IntegrationProperties.getExpressionFor(IntegrationProperties.CHANNELS_AUTOCREATE);
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder.genericBeanDefinition(ChannelInitializer.class)
					.addPropertyValue("autoCreate", channelsAutoCreateExpression);
			BeanDefinitionHolder channelCreatorHolder = new BeanDefinitionHolder(channelDef.getBeanDefinition(),
					IntegrationContextUtils.CHANNEL_INITIALIZER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelCreatorHolder, registry);
		}

		if (!registry.containsBeanDefinition(IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME)) {
			BeanDefinitionBuilder channelRegistryBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ChannelInitializer.AutoCreateCandidatesCollector.class);
			channelRegistryBuilder.addConstructorArgValue(new ManagedSet<String>());
			channelRegistryBuilder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			BeanDefinitionHolder channelRegistryHolder =
					new BeanDefinitionHolder(channelRegistryBuilder.getBeanDefinition(),
							IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelRegistryHolder, registry);
		}
	}

}
