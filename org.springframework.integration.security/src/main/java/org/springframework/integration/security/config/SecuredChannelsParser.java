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

import java.util.List;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.security.channel.ChannelAccessPolicy;
import org.springframework.integration.security.channel.ChannelInvocationDefinitionSource;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Creates a {@link ChannelSecurityInterceptor} to control send and receive access,
 * and creates a {@link ChannelSecurityInterceptorBeanPostProcessor} to apply the
 * interceptor to {@link MessageChannel}s whose names match the specified patterns.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class SecuredChannelsParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return ChannelSecurityInterceptorBeanPostProcessor.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		ChannelInvocationDefinitionSource objectDefinitionSource = this.parseObjectDefinitionSource(element);
		BeanDefinitionBuilder interceptorBuilder = BeanDefinitionBuilder.genericBeanDefinition(ChannelSecurityInterceptor.class);
		interceptorBuilder.addConstructorArgValue(objectDefinitionSource);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(interceptorBuilder, element, "authentication-manager");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(interceptorBuilder, element, "access-decision-manager");
		String interceptorBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				interceptorBuilder.getBeanDefinition(), parserContext.getRegistry());
		builder.addConstructorArgReference(interceptorBeanName);
	}


	@SuppressWarnings("unchecked")
	private ChannelInvocationDefinitionSource parseObjectDefinitionSource(Element element) {
		ChannelInvocationDefinitionSource objectDefinitionSource = new ChannelInvocationDefinitionSource();
		List<Element> accessPolicyElements = (List<Element>) DomUtils.getChildElementsByTagName(element, "access-policy");
		for (Element accessPolicyElement : accessPolicyElements) {
			Pattern pattern = Pattern.compile(accessPolicyElement.getAttribute("pattern"));
			String sendAccess = accessPolicyElement.getAttribute("send-access");
			String receiveAccess = accessPolicyElement.getAttribute("receive-access");
			Assert.isTrue(StringUtils.hasText(sendAccess) || StringUtils.hasText(receiveAccess),
					"At least one of 'send-access' or 'receive-access' must be provided.");
			objectDefinitionSource.addPatternMapping(pattern, new ChannelAccessPolicy(sendAccess, receiveAccess));
		}
		return objectDefinitionSource;
	}

}
