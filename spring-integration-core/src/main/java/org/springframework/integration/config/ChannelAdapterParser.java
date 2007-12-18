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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.endpoint.InboundMethodInvokingChannelAdapter;
import org.springframework.integration.endpoint.OutboundMethodInvokingChannelAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for inbound and outbound channel adapters.
 * 
 * @author Mark Fisher
 */
public class ChannelAdapterParser implements BeanDefinitionParser {

	private static final String ID_ATTRIBUTE = "id";

	private static final String REF_ATTRIBUTE = "ref";

	private static final String METHOD_ATTRIBUTE = "method";


	private final boolean isInbound; 


	public ChannelAdapterParser(boolean isInbound) {
		this.isInbound = isInbound;
	}


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		RootBeanDefinition adapterDef = null;
		if (this.isInbound) {
			adapterDef = new RootBeanDefinition(InboundMethodInvokingChannelAdapter.class);
		}
		else {
			adapterDef = new RootBeanDefinition(OutboundMethodInvokingChannelAdapter.class);
		}
		adapterDef.setSource(parserContext.extractSource(element));
		String ref = element.getAttribute(REF_ATTRIBUTE);
		String method = element.getAttribute(METHOD_ATTRIBUTE);
		if (!StringUtils.hasText(ref) || !StringUtils.hasText(method)) {
			throw new MessagingConfigurationException("'ref' and 'method' are both required");
		}
		adapterDef.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(ref));
		adapterDef.getPropertyValues().addPropertyValue("method", method);
		String beanName = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(beanName)) {
			beanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		}
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, beanName));
		return adapterDef;
	}

}
