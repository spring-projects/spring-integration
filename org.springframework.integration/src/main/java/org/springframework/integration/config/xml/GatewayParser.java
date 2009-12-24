/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.List;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.integration.gateway.GatewayMethodDefinition;
import org.springframework.util.ObjectUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;gateway/&gt; element.
 * 
 * @author Mark Fisher
 */
public class GatewayParser extends AbstractSimpleBeanDefinitionParser {

	private static String[] referenceAttributes = new String[] {
		"default-request-channel", "default-reply-channel", "message-mapper"
	};

	private static String[] innerAttributes = new String[] {
		"request-channel", "reply-channel", "request-timeout", "reply-timeout"
	};


	@Override
	protected String getBeanClassName(Element element) {
		return IntegrationNamespaceUtils.BASE_PACKAGE + ".gateway.GatewayProxyFactoryBean";
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !ObjectUtils.containsElement(referenceAttributes, attributeName)
				&& !ObjectUtils.containsElement(innerAttributes, attributeName)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
		if ("chain".equals(element.getParentNode().getLocalName())) {
			this.postProcessInnerGateway(builder, element);
		}
		else {
			this.postProcessGateway(builder, element);
		}
	}

	private void postProcessInnerGateway(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel", "defaultRequestChannel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "defaultReplyChannel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout", "defaultRequestTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "defaultReplyTimeout");
	}

	private void postProcessGateway(BeanDefinitionBuilder builder, Element element) {
		for (String attributeName : referenceAttributes) {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, attributeName);
		}
		List<Element> elements = DomUtils.getChildElementsByTagName(element, "method");
		ManagedMap<String, GatewayMethodDefinition> methodToChannelMap = null;
		if (elements != null && elements.size() > 0){
			methodToChannelMap = new ManagedMap<String, GatewayMethodDefinition>();
		}
		for (Element methodElement : elements) {
			String methodName = methodElement.getAttribute("name");
			GatewayMethodDefinition gatewayDefinition = new GatewayMethodDefinition();
			gatewayDefinition.setRequestChannelName(methodElement.getAttribute("request-channel"));
			gatewayDefinition.setReplyChannelName(methodElement.getAttribute("reply-channel"));
			gatewayDefinition.setRequestTimeout(methodElement.getAttribute("request-timeout"));
			gatewayDefinition.setReplyTimeout(methodElement.getAttribute("reply-timeout"));	
			methodToChannelMap.put(methodName, gatewayDefinition);
		}
		builder.addPropertyValue("methodToChannelMap", methodToChannelMap);
	}

}
