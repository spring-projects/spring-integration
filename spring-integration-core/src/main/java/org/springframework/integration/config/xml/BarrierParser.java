/*
 * Copyright 2015 the original author or authors.
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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.SuspendingMessageHandler;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class BarrierParser extends AbstractBeanDefinitionParser {

	private String releaseHandlerBeanName;

	private String releaserBeanName;

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		new ReleaserParser().parse(element, parserContext);
		BeanDefinition consumerDef = parserContext.getRegistry().getBeanDefinition(this.releaserBeanName);
		MutablePropertyValues propertyValues = consumerDef.getPropertyValues();
		// remove properties that only apply to the primary endpoint
		// release channel cannot be pollable
		if (propertyValues.get("pollerMetadata") != null) {
			propertyValues.removePropertyValue("pollerMetadata");
		}
		Object handlerProperty = propertyValues.get("handler");
		Assert.notNull(handlerProperty, "no handler found for release consumer");
		Assert.isTrue(handlerProperty instanceof RuntimeBeanReference, "handler bean reference invalid");
		this.releaseHandlerBeanName = ((RuntimeBeanReference) handlerProperty).getBeanName();
		BeanDefinition releaseHandler = parserContext.getRegistry().getBeanDefinition(releaseHandlerBeanName);
		propertyValues = releaseHandler.getPropertyValues();
		// remove properties that only apply to the primary endpoint
		if (propertyValues.get("outputChannel") != null) {
			propertyValues.removePropertyValue("outputChannel");
		}
 		new SuspenderParser().parse(element, parserContext);
		return null;
	}


	private class SuspenderParser extends AbstractConsumerEndpointParser {

		@Override
		protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
			BeanDefinitionBuilder handlerBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(SuspendingMessageHandler.class);
			handlerBuilder.addConstructorArgReference(releaseHandlerBeanName);
			IntegrationNamespaceUtils.injectConstructorWithAdapter("correlation-strategy",
					"correlation-strategy-method", "correlation-strategy-expression",
					"CorrelationStrategy", element, handlerBuilder, null, parserContext);
			handlerBuilder.addConstructorArgValue(element.getAttribute("timeout"));
			return handlerBuilder;
		}

	}

	private class ReleaserParser extends AbstractConsumerEndpointParser {

		@Override
		protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
				throws BeanDefinitionStoreException {
			releaserBeanName = super.resolveId(element, definition, parserContext) + ".releaser";
			return releaserBeanName;
		}

		@Override
		protected String getInputChannelAttributeName() {
			return "release-channel";
		}

		@Override
		protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
			return BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.integration.handler.SuspendingMessageHandler$ReleasingMessageHandler");
		}

	}

}

