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

package org.springframework.integration.transformer.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.PayloadTransformer;
import org.springframework.integration.transformer.PayloadTransformingMessageHandler;

/**
 * @author Mark Fisher
 */
public abstract class AbstractPayloadTransformerParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return PayloadTransformingMessageHandler.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BeanDefinitionBuilder transformerBuilder = BeanDefinitionBuilder.genericBeanDefinition(this.getTransformerClass());
		this.parsePayloadTransformer(element, parserContext, transformerBuilder);
		String transformerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				transformerBuilder.getBeanDefinition(), parserContext.getRegistry());
		builder.addConstructorArgReference(transformerBeanName);
	}

	protected abstract Class<? extends PayloadTransformer<?, ?>> getTransformerClass();

	protected abstract void parsePayloadTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder);

}
