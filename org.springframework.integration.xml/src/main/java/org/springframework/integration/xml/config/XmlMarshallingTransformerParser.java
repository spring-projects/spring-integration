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
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.integration.xml.transformer.XmlPayloadMarshallingTransformer;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class XmlMarshallingTransformerParser extends AbstractSingleBeanDefinitionParser {

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
		String resourceFactory = element.getAttribute("result-factory");
		String marshaller = element.getAttribute("marshaller");

		Assert.hasText(marshaller, "A unmarshaller attribute is required");

		Assert.hasText(resourceFactory, "A result-factory attribute is required");

		builder.getBeanDefinition().setBeanClass(XmlPayloadMarshallingTransformer.class);

		builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(
				new RuntimeBeanReference(marshaller));

		if (resourceFactory.equals("DOMResult")) {
			builder.getBeanDefinition().getPropertyValues().addPropertyValue("resultFactory", new DomResultFactory());
		}
		else if (resourceFactory.equals("StringResult")) {
			builder.getBeanDefinition().getPropertyValues()
					.addPropertyValue("resultFactory", new StringResultFactory());
		}
	}

}
