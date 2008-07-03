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

package org.springframework.integration.security.endpoint.config;

import org.springframework.beans.BeanMetadataAttribute;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.security.endpoint.SecurityEndpointInterceptor;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class SecurityEndpointInterceptorParser extends AbstractSingleBeanDefinitionParser {

	public SecurityEndpointInterceptorParser() {
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

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String access = element.getAttribute("access");
		Assert.hasText(access, "Access attribute is required for element endpoint-security-policy");

		String accessDecisionManager = element.getAttribute("access-decision-manager");
		Assert.hasText(accessDecisionManager, "A non null value for the access-decision-manager is required");

		ConfigAttributeDefinition accessDefintion = new ConfigAttributeDefinition(StringUtils.tokenizeToStringArray(
				access, ","));
		builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(
				new ValueHolder(accessDefintion));
		builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(
				new RuntimeBeanReference(accessDecisionManager));

		builder.getBeanDefinition().setBeanClass(SecurityEndpointInterceptor.class);
		String beanName = BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(),
				parserContext.getRegistry());
		builder.getBeanDefinition().addMetadataAttribute(new BeanMetadataAttribute("interceptorName", beanName));
	}

}
