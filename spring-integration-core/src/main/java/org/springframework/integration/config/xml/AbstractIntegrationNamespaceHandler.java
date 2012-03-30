/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.ChannelInitializer.AutoCreateCandidatesCollector;
import org.springframework.util.StringUtils;

/**
 * Base class for NamespaceHandlers that registers a BeanFactoryPostProcessor
 * for configuring default bean definitions.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public abstract class AbstractIntegrationNamespaceHandler implements NamespaceHandler {

	private static final String VERSION = "2.1";

	public static final String CHANNEL_INITIALIZER_BEAN_NAME = ChannelInitializer.class.getSimpleName();

	private static final String DEFAULT_CONFIGURING_POSTPROCESSOR_SIMPLE_CLASS_NAME =
			"DefaultConfiguringBeanFactoryPostProcessor";

	private static final String DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME =
			IntegrationNamespaceUtils.BASE_PACKAGE + ".internal" + DEFAULT_CONFIGURING_POSTPROCESSOR_SIMPLE_CLASS_NAME;


	private final NamespaceHandlerDelegate delegate = new NamespaceHandlerDelegate();


	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		this.verifySchemaVersion(element, parserContext);
		this.registerImplicitChannelCreator(parserContext);
		this.registerDefaultConfiguringBeanFactoryPostProcessorIfNecessary(parserContext);
		return this.delegate.parse(element, parserContext);
	}

	public final BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext) {
		return this.delegate.decorate(source, definition, parserContext);
	}

	/*
	 * This method will auto-register a ChannelInitializer which could also be overridden by the user 
	 * by simply registering a ChannelInitializer <bean> with its 'autoCreate' property set to false to suppress channel creation.
	 * It will also register a ChannelInitializer$AutoCreateCandidatesCollector which simply collects candidate channel names.
	 */
	private void registerImplicitChannelCreator(ParserContext parserContext) {
		// ChannelInitializer
		boolean alreadyRegistered = false;
		if (parserContext.getRegistry() instanceof ListableBeanFactory) {
			// unlike DefaultConfiguringBeanFactoryPostProcessor we need one of these per registry
			// therefore we need to call containsBeanDefinition(..) which does not consider parent registry
			alreadyRegistered = ((ListableBeanFactory) parserContext.getRegistry()).containsBeanDefinition(CHANNEL_INITIALIZER_BEAN_NAME);
		}
		else {
			alreadyRegistered = parserContext.getRegistry().isBeanNameInUse(CHANNEL_INITIALIZER_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			BeanDefinitionBuilder channelDef = BeanDefinitionBuilder.genericBeanDefinition(ChannelInitializer.class);
			BeanDefinitionHolder channelCreatorHolder = new BeanDefinitionHolder(channelDef.getBeanDefinition(), CHANNEL_INITIALIZER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelCreatorHolder, parserContext.getRegistry());
		}
		// ChannelInitializer$AutoCreateCandidatesCollector
		alreadyRegistered = false;
		if (parserContext.getRegistry() instanceof ListableBeanFactory) {
			// unlike DefaultConfiguringBeanFactoryPostProcessor we need one of these per registry
			// therefore we need to call containsBeanDefinition(..) which does not consider parent registry
			alreadyRegistered = ((ListableBeanFactory) parserContext.getRegistry()).
					containsBeanDefinition(ChannelInitializer.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
		}
		else {
			alreadyRegistered = parserContext.getRegistry().isBeanNameInUse(ChannelInitializer.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			BeanDefinitionBuilder channelRegistryBuilder = BeanDefinitionBuilder.genericBeanDefinition(AutoCreateCandidatesCollector.class);
			channelRegistryBuilder.addConstructorArgValue(new ManagedSet<String>());
			BeanDefinitionHolder channelRegistryHolder = 
					new BeanDefinitionHolder(channelRegistryBuilder.getBeanDefinition(), ChannelInitializer.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(channelRegistryHolder, parserContext.getRegistry());
		}
	}

	private void registerDefaultConfiguringBeanFactoryPostProcessorIfNecessary(ParserContext parserContext) {
		boolean alreadyRegistered = false;
		if (parserContext.getRegistry() instanceof ListableBeanFactory) {
			alreadyRegistered = ((ListableBeanFactory) parserContext.getRegistry()).containsBean(DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
		}
		else {
			alreadyRegistered = parserContext.getRegistry().isBeanNameInUse(DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
		}
		if (!alreadyRegistered) {
			BeanDefinitionBuilder postProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".config.xml." + DEFAULT_CONFIGURING_POSTPROCESSOR_SIMPLE_CLASS_NAME);
			BeanDefinitionHolder postProcessorHolder = new BeanDefinitionHolder(
					postProcessorBuilder.getBeanDefinition(), DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(postProcessorHolder, parserContext.getRegistry());
		}
	}

	protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator decorator) {
		this.delegate.doRegisterBeanDefinitionDecorator(elementName, decorator);
	}

	protected final void registerBeanDefinitionDecoratorForAttribute(String attributeName, BeanDefinitionDecorator decorator) {
		this.delegate.doRegisterBeanDefinitionDecoratorForAttribute(attributeName, decorator);
	}

	protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
		this.delegate.doRegisterBeanDefinitionParser(elementName, parser);
	}

	private void verifySchemaVersion(Element element, ParserContext parserContext) {
		if (!(matchesVersion(element) && matchesVersion(element.getOwnerDocument().getDocumentElement())))  {
			parserContext.getReaderContext().error(
					"You cannot use prior versions of Spring Integration schemas with Spring Integration " + VERSION  +
					". Please upgrade your schema declarations or use versionless aliases (e.g. spring-integration.xsd).", element);
		}
	}

	private static boolean matchesVersion(Element element) {
		String schemaLocation = element.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
		return !StringUtils.hasText(schemaLocation) // no namespace on this element
				|| schemaLocation.matches("(?m).*spring-integration-[a-z-]*" + VERSION + ".xsd.*") // correct version
				|| schemaLocation.matches("(?m).*spring-integration[a-z-]*.xsd.*") // version-less schema
				|| !schemaLocation.matches("(?m).*spring-integration.*"); // no spring-integration schemas
	}

	private class NamespaceHandlerDelegate extends NamespaceHandlerSupport {

		public void init() {
			AbstractIntegrationNamespaceHandler.this.init();
		}

		private void doRegisterBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator decorator) {
			super.registerBeanDefinitionDecorator(elementName, decorator);
		}

		private void doRegisterBeanDefinitionDecoratorForAttribute(String attributeName, BeanDefinitionDecorator decorator) {
			super.registerBeanDefinitionDecoratorForAttribute(attributeName, decorator);
		}

		private void doRegisterBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
			super.registerBeanDefinitionParser(elementName, parser);
		}

	}
}
