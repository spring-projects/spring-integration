/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.config.xml;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.flow.ChannelNamePortConfiguration;
import org.springframework.integration.flow.FlowConfiguration;
import org.springframework.integration.flow.PortMetadata;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parse the {@link FlowConfiguration}
 * 
 * @author David Turanski
 * 
 */
public class FlowConfigurationParser implements BeanDefinitionParser {

	public BeanDefinition parse(Element element, ParserContext parserContext) {

		List<Element> portMappings = DomUtils.getChildElementsByTagName(element, "port-mapping");

		BeanDefinitionBuilder flowConfigurationBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(FlowConfiguration.class);

		ManagedList<Object> portConfigList = new ManagedList<Object>();

		for (Element el : portMappings) {
			if (!DomUtils.getChildElements(el).isEmpty()) {
				if (el.hasAttribute("input-channel") || el.hasAttribute("output-channel")) {
					parserContext.getReaderContext().error(
							"port-mapping cannot include both channel attributes and child elements",
							flowConfigurationBuilder);
				}
				BeanDefinition portConfiguration = buildFlowProviderPortConfiguration(el, parserContext);
				portConfigList.add(portConfiguration);
			}
			else {
				// A default port configuration
				if (!(el.hasAttribute("input-channel"))) {
					parserContext.getReaderContext().error(
							"port-mapping with no child elements must include an 'input-channel' attribute",
							flowConfigurationBuilder);
				}

				BeanDefinitionBuilder portConfigurationBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(ChannelNamePortConfiguration.class);

				portConfigurationBuilder.addConstructorArgValue(el.getAttribute("input-channel"));
				if (el.hasAttribute("output-channel")) {
					portConfigurationBuilder.addConstructorArgValue(el.getAttribute("output-channel"));
				}
				else {
					portConfigurationBuilder.addConstructorArgValue(null);
				}
				portConfigList.add(portConfigurationBuilder.getBeanDefinition());
			}
		}

		flowConfigurationBuilder.addConstructorArgValue(portConfigList);

		BeanDefinitionReaderUtils.registerWithGeneratedName(flowConfigurationBuilder.getBeanDefinition(),
				parserContext.getRegistry());

		return null;
	}

	private BeanDefinition buildFlowProviderPortConfiguration(Element el, ParserContext parserContext) {
		Element inputPortEl = DomUtils.getChildElementByTagName(el, "input-port");

		BeanDefinitionBuilder portConfigurationBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(ChannelNamePortConfiguration.class);

		BeanDefinition portMetadata = this.buildPortMetadata(el, inputPortEl);

		portConfigurationBuilder.addConstructorArgValue(portMetadata);

		List<Element> outputPortElements = DomUtils.getChildElementsByTagName(el, "output-port");
		ManagedList<Object> outputList = null;

		if (outputPortElements != null) {

			outputList = new ManagedList<Object>();
			for (Element outputPortEl : outputPortElements) {
				portMetadata = this.buildPortMetadata(el, outputPortEl);
				outputList.add(portMetadata);
			}
		}

		portConfigurationBuilder.addConstructorArgValue(outputList);

		return portConfigurationBuilder.getBeanDefinition();
	}

	private BeanDefinition buildPortMetadata(Element element, Element portElement) {
		BeanDefinitionBuilder portMetadataBuilder = BeanDefinitionBuilder.genericBeanDefinition(PortMetadata.class);
		portMetadataBuilder.addConstructorArgValue(portElement.getAttribute("name"));
		portMetadataBuilder.addConstructorArgValue(portElement.getAttribute("channel"));
		return portMetadataBuilder.getBeanDefinition();
	}

}
