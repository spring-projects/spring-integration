/*
 * Copyright 2002-2019 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.MessageTransformingHandler;

/**
 * @author Mark Fisher
 */
public abstract class AbstractTransformerParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				MessageTransformingHandler.class);
		BeanDefinitionBuilder transformerBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(this.getTransformerClassName());
		this.parseTransformer(element, parserContext, transformerBuilder);
		String transformerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				transformerBuilder.getBeanDefinition(), parserContext.getRegistry());
		builder.addConstructorArgReference(transformerBeanName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder;
	}

	protected abstract String getTransformerClassName();

	protected abstract void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder);

}
