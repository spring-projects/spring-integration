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

package org.springframework.integration.groovy.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class GroovyScriptParser extends AbstractSingleBeanDefinitionParser {

	private static final String LOCATION_ATTRIBUTE = "location";

	private static final String REFRESH_CHECK_DELAY_ATTRIBUTE = "refresh-check-delay";


	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addConstructorArgValue(this.resolveScriptSource(element, parserContext.getReaderContext()));
		BeanDefinitionBuilder scriptVariableSource = 
			BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.groovy.DefaultScriptVariableSource");
		String name = 
			BeanDefinitionReaderUtils.registerWithGeneratedName(scriptVariableSource.getBeanDefinition(), parserContext.getRegistry());
		builder.addConstructorArgReference(name);
	}

	private Object resolveScriptSource(Element element, XmlReaderContext readerContext) {
		boolean hasScriptLocation = element.hasAttribute(LOCATION_ATTRIBUTE);
		String scriptText = DomUtils.getTextValue(element);
		if (!(hasScriptLocation ^ StringUtils.hasText(scriptText))) {
			readerContext.error("Either the 'location' attribute or inline script text must be provided, but not both.", element);
			return null;
		}
		else if (hasScriptLocation) {
			String refreshDelayText = element.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);			
			String beanClassName = "org.springframework.integration.groovy.config.RefreshableResourceScriptSource";
			BeanDefinitionBuilder resourceScriptSourceBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(beanClassName);
			resourceScriptSourceBuilder.addConstructorArgValue(element.getAttribute(LOCATION_ATTRIBUTE));
			if (StringUtils.hasText(refreshDelayText)) {
				resourceScriptSourceBuilder.addConstructorArgValue(refreshDelayText);
			}
			else {
				resourceScriptSourceBuilder.addConstructorArgValue(-1L);				
			}
			return resourceScriptSourceBuilder.getBeanDefinition();
		}
		return new StaticScriptSource(scriptText, "groovy.lang.Script");
	}

}
