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

package org.springframework.integration.xml.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class IntegrationXmlNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("marshalling-transformer", new MarshallingTransformerParser());
		registerBeanDefinitionParser("unmarshalling-transformer", new UnmarshallingTransformerParser());
		registerBeanDefinitionParser("xslt-transformer", new XsltPayloadTransformerParser());
		registerBeanDefinitionParser("xpath-transformer", new XPathTransformerParser());
		registerBeanDefinitionParser("xpath-header-enricher", new XPathHeaderEnricherParser());
		registerBeanDefinitionParser("xpath-router", new XPathRouterParser());
		registerBeanDefinitionParser("xpath-filter", new XPathFilterParser());
		registerBeanDefinitionParser("xpath-expression", new XPathExpressionParser());
		registerBeanDefinitionParser("xpath-splitter", new XPathMessageSplitterParser());
		registerBeanDefinitionParser("validating-filter", new XmlPayloadValidatingFilterParser());
	}

}
