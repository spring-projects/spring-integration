/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Creates a {@link org.springframework.integration.security.channel.ChannelSecurityInterceptor}
 * to control send and receive access, and creates a bean post-processor to apply the
 * interceptor to {@link org.springframework.messaging.MessageChannel}s
 * whose names match the specified patterns.
 * 
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class SecuredChannelsParser extends AbstractSingleBeanDefinitionParser {

	private final static String BASE_PACKAGE_NAME = "org.springframework.integration.security";


	@Override
	protected String getBeanClassName(Element element) {
		return BASE_PACKAGE_NAME + ".config.ChannelSecurityInterceptorBeanPostProcessor";
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String securityMetadataSourceBeanName = this.parseSecurityMetadataSource(element, parserContext);
		BeanDefinitionBuilder interceptorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				BASE_PACKAGE_NAME + ".channel.ChannelSecurityInterceptor");
		interceptorBuilder.addConstructorArgReference(securityMetadataSourceBeanName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(interceptorBuilder, element, "authentication-manager");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(interceptorBuilder, element, "access-decision-manager");
		String interceptorBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				interceptorBuilder.getBeanDefinition(), parserContext.getRegistry());
		builder.addConstructorArgReference(interceptorBeanName);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String parseSecurityMetadataSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				BASE_PACKAGE_NAME + ".channel.ChannelSecurityMetadataSource");
		List<Element> accessPolicyElements = DomUtils.getChildElementsByTagName(element, "access-policy");
		ManagedMap patternMappings = new ManagedMap();
		for (Element accessPolicyElement : accessPolicyElements) {
			Pattern pattern = Pattern.compile(accessPolicyElement.getAttribute("pattern"));
			String sendAccess = accessPolicyElement.getAttribute("send-access");
			String receiveAccess = accessPolicyElement.getAttribute("receive-access");
			if (!StringUtils.hasText(sendAccess) && !StringUtils.hasText(receiveAccess)) {
				parserContext.getReaderContext().error(
						"At least one of 'send-access' or 'receive-access' must be provided.", accessPolicyElement);
			}
			BeanDefinitionBuilder accessPolicyBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					BASE_PACKAGE_NAME + ".channel.DefaultChannelAccessPolicy");
			accessPolicyBuilder.addConstructorArgValue(sendAccess);
			accessPolicyBuilder.addConstructorArgValue(receiveAccess);
			accessPolicyBuilder.getBeanDefinition().setRole(BeanDefinition.ROLE_SUPPORT);
			patternMappings.put(pattern, accessPolicyBuilder.getBeanDefinition());
		}
		builder.addConstructorArgValue(patternMappings);
		builder.setRole(BeanDefinition.ROLE_SUPPORT);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
