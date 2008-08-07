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

package org.springframework.integration.xml.config;

import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.PayloadTransformer;
import org.springframework.integration.transformer.config.AbstractPayloadTransformerParser;
import org.springframework.integration.xml.result.DomResultFactory;
import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.integration.xml.transformer.XmlPayloadMarshallingTransformer;
import org.springframework.util.Assert;
import org.w3c.dom.Element;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class XmlMarshallingTransformerParser extends
		AbstractPayloadTransformerParser {

	@Override
	protected Class<? extends PayloadTransformer<?, ?>> getTransformerClass() {
		return XmlPayloadMarshallingTransformer.class;
	}

	@Override
	protected void parsePayloadTransformer(Element element,
			ParserContext parserContext, BeanDefinitionBuilder builder) {
		String resultFactory = element.getAttribute("result-factory");
		String marshaller = element.getAttribute("marshaller");
		Assert.hasText(marshaller, "the 'marshaller' attribute is required");
		Assert.hasText(resultFactory,
				"the 'result-factory' attribute is required");
		builder.addConstructorArgReference(marshaller);

		if (resultFactory.equals("DOMResult")) {
			try {
				builder.addPropertyValue("resultFactory",
						new DomResultFactory());

			} catch (ParserConfigurationException e) {
				throw new org.springframework.integration.ConfigurationException(
						"Exception creating DomResultFactory");
			}
		} else if (resultFactory.equals("StringResult")) {
			builder
					.addPropertyValue("resultFactory",
							new StringResultFactory());
		}
	}
}
