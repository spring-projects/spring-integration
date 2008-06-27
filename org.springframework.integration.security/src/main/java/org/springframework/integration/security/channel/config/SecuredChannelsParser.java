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

package org.springframework.integration.security.channel.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.security.ChannelInterceptorRegisteringBeanPostProcessor;
import org.springframework.integration.security.channel.SecurityEnforcingChannelInterceptor;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.context.SecurityContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Determines {@link SecurityContext} propagation behaviour for the parent
 * element channel, and creates a {@link SecurityEnforcingChannelInterceptor} to
 * control send and receive access if send-access and/or receive-access is
 * specified.
 * 
 * @author Jonas Partner
 */
public class SecuredChannelsParser extends AbstractSingleBeanDefinitionParser {

	public SecuredChannelsParser() {
		super();
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
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

		BeanDefinition interceptorBeanDefinition = createSecurityEnforcingChannelInterceptor(accessDecisionManager,
				sendAccess, receiveAccess);

		List<String> patternList = processPatterns(element.getElementsByTagNameNS(element.getNamespaceURI(),
				"channel-name-pattern"));

		builder.getBeanDefinition().setBeanClass(ChannelInterceptorRegisteringBeanPostProcessor.class);
		builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(
				new ValueHolder(interceptorBeanDefinition));
		builder.getBeanDefinition().getConstructorArgumentValues()
				.addGenericArgumentValue(new ValueHolder(patternList));

	}

	protected List<String> processPatterns(NodeList patternList) {
		List<String> patterns = new ArrayList<String>();
		for (int i = 0; i < patternList.getLength(); i++) {
			Element patternElement = (Element) patternList.item(i);
			patterns.add(patternElement.getTextContent());
		}
		return patterns;
	}

	protected BeanDefinition createSecurityEnforcingChannelInterceptor(String accessDecisionManager, String sendAccess,
			String receiveAccess) {
		if (!StringUtils.hasText(accessDecisionManager)) {
			accessDecisionManager = "accessDecisionManager";
		}
		BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(SecurityEnforcingChannelInterceptor.class);
		beanDefinitionBuilder.addConstructorArgReference(accessDecisionManager);

		if (StringUtils.hasText(sendAccess)) {
			ConfigAttributeDefinition sendDefinition = new ConfigAttributeDefinition(StringUtils.tokenizeToStringArray(
					sendAccess, ","));
			beanDefinitionBuilder.addPropertyValue("sendSecurityAttributes", sendDefinition);
		}
		if (StringUtils.hasText(receiveAccess)) {
			ConfigAttributeDefinition receiveDefinition = new ConfigAttributeDefinition(StringUtils
					.tokenizeToStringArray(receiveAccess, ","));
			beanDefinitionBuilder.addPropertyValue("receiveSecurityAttributes", receiveDefinition);
		}
		return beanDefinitionBuilder.getBeanDefinition();

	}

}
