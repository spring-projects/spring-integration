/*
 * Copyright 2002-2014 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.xml.BeanDefinitionDecorator;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.IntegrationRegistrar;
import org.springframework.util.StringUtils;

/**
 * Base class for NamespaceHandlers that registers a BeanFactoryPostProcessor
 * for configuring default bean definitions.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractIntegrationNamespaceHandler implements NamespaceHandler {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private static final String VERSION = "4.1";

	private final NamespaceHandlerDelegate delegate = new NamespaceHandlerDelegate();


	@Override
	public final BeanDefinition parse(Element element, ParserContext parserContext) {
		this.verifySchemaVersion(element, parserContext);
		new IntegrationRegistrar().registerBeanDefinitions(null, parserContext.getRegistry());
		return this.delegate.parse(element, parserContext);
	}

	@Override
	public final BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext) {
		return this.delegate.decorate(source, definition, parserContext);
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
		if (!(matchesVersion(element) && matchesVersion(element.getOwnerDocument().getDocumentElement()))) {
			parserContext.getReaderContext().error(
					"You cannot use prior versions of Spring Integration schemas with Spring Integration " + VERSION +
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

		@Override
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
