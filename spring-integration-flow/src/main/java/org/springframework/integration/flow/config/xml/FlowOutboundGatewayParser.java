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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.flow.config.FlowMessageHandlerFactoryBean;
import org.w3c.dom.Element;

/**
 * 
 * @author David Turanski
 * 
 */
public class FlowOutboundGatewayParser extends AbstractConsumerEndpointParser {
 
	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder flowHandlerBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(FlowMessageHandlerFactoryBean.class);
		String flowName = element.getAttribute("flow");
		flowHandlerBuilder.addPropertyReference("flow", flowName);
	 
		IntegrationNamespaceUtils.setValueIfAttributeDefined(flowHandlerBuilder, element, "input-port","inputPortName");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(flowHandlerBuilder, element, "timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(flowHandlerBuilder, element, "error-channel");
		
		return flowHandlerBuilder;
	}
}
