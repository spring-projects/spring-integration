/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.xml.config;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.xml.transformer.XmlPayloadUnmarshallingTransfomer;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class XmlUnmarshallingTransformerParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String unmarshaller = element.getAttribute("unmarshaller");
		Assert.hasText(unmarshaller, "A unmarshaller attribute is required");
		builder.getBeanDefinition().setBeanClass(XmlPayloadUnmarshallingTransfomer.class);
		builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(
				new RuntimeBeanReference(unmarshaller));
	}

}
