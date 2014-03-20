/*
 * Copyright 2002-2014 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.ChannelSecurityMetadataSource;
import org.springframework.integration.security.channel.DefaultChannelAccessPolicy;
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
 * @author Artem Bilan
 */
public class SecuredChannelsParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return ChannelSecurityInterceptor.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addConstructorArgValue(this.parseSecurityMetadataSource(element, parserContext));
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "authentication-manager");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "access-decision-manager");
	}

	private BeanDefinition parseSecurityMetadataSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChannelSecurityMetadataSource.class);
		List<Element> accessPolicyElements = DomUtils.getChildElementsByTagName(element, "access-policy");
		ManagedMap<String, BeanDefinition> patternMappings = new ManagedMap<String, BeanDefinition>();
		for (Element accessPolicyElement : accessPolicyElements) {
			String sendAccess = accessPolicyElement.getAttribute("send-access");
			String receiveAccess = accessPolicyElement.getAttribute("receive-access");
			if (!StringUtils.hasText(sendAccess) && !StringUtils.hasText(receiveAccess)) {
				parserContext.getReaderContext().error(
						"At least one of 'send-access' or 'receive-access' must be provided.", accessPolicyElement);
			}
			BeanDefinitionBuilder accessPolicyBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultChannelAccessPolicy.class);
			accessPolicyBuilder.addConstructorArgValue(sendAccess);
			accessPolicyBuilder.addConstructorArgValue(receiveAccess);
			accessPolicyBuilder.getBeanDefinition().setRole(BeanDefinition.ROLE_SUPPORT);
			patternMappings.put(accessPolicyElement.getAttribute("pattern"), accessPolicyBuilder.getBeanDefinition());
		}
		builder.addConstructorArgValue(patternMappings);

		return builder.getBeanDefinition();
	}

}
