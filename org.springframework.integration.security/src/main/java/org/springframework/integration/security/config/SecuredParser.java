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

package org.springframework.integration.security.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.security.SecurityEnforcingChannelInterceptor;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContext;
import org.springframework.util.StringUtils;

/**
 * Determines {@link SecurityContext} propagation behaviour for the parent element
 * channel, and creates a {@link SecurityEnforcingChannelInterceptor} to control
 * send and receive access if send-access and/or receive-access is specified.
 *  
 * @author Jonas Partner
 */
public class SecuredParser extends AbstractSingleBeanDefinitionParser {

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
		String receiveAccess = element.getAttribute("receive-access");
		String sendAccess = element.getAttribute("send-access");
		String accessDecisionManager = element.getAttribute("access-decision-manager");
		String propagation = element.getAttribute("propagate");
		String channelName = ((Element)element.getParentNode()).getAttribute("id");
		if (channelName == null) {
			parserContext.getReaderContext().error("The secured element requires a channel parent id.", element);
		}
		builder.getBeanDefinition().setBeanClass(SecurityEnforcingChannelInterceptor.class);
		if (!StringUtils.hasText(accessDecisionManager)) {
			accessDecisionManager = "accessDecisionManager";
		}
		builder.addConstructorArgReference(accessDecisionManager);
		builder.addConstructorArgReference(channelName);
		if (StringUtils.hasText(sendAccess)) {
			ConfigAttributeDefinition sendDefinition = new ConfigAttributeDefinition(sendAccess);
			builder.addPropertyValue("sendSecurityAttributes", sendDefinition);
		}
		if (StringUtils.hasText(receiveAccess)) {
			ConfigAttributeDefinition receiveDefinition = new ConfigAttributeDefinition(receiveAccess);
			builder.addPropertyValue("receiveSecurityAttributes", receiveDefinition);
		}
		boolean propagationValue = true;
		if (StringUtils.hasText(propagation)) {
			propagationValue = Boolean.parseBoolean(propagation);
		}
		if (propagationValue) {
			SecurityPropagatingBeanPostProcessorDefinitionHelper.addToIncludeChannelList(channelName, parserContext);
		}
		else {
			SecurityPropagatingBeanPostProcessorDefinitionHelper.addToExcludeChannelList(channelName, parserContext);
		}
	}

}
