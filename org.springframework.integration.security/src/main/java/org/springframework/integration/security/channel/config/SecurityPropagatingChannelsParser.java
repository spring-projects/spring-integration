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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.security.config.IncludeExcludePatternParser;
import org.springframework.integration.security.config.OrderedIncludeExcludeList;
import org.springframework.security.context.SecurityContext;
import org.w3c.dom.Element;

/**
 * Interprets the &lt;secure-channels&gt; element which controls default
 * {@link SecurityContext} propagation behaviour.
 * 
 * @author Jonas Partner
 */
public class SecurityPropagatingChannelsParser extends AbstractSingleBeanDefinitionParser {

	IncludeExcludePatternParser includeExcludePatternParser = new IncludeExcludePatternParser();

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		boolean propagateByDefault = Boolean.parseBoolean(element.getAttribute("propagate-by-default"));
		OrderedIncludeExcludeList includeExcludeList = includeExcludePatternParser.createFromNodeList(
				propagateByDefault, element.getChildNodes());
		builder.getBeanDefinition().setBeanClass(SecurityPropagatingBeanPostProcessor.class);
		builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(
				new ValueHolder(includeExcludeList));

	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		return "internal.integration.SecureChannels";
	}

}
