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

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
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
		String scriptLocation = element.getAttribute(LOCATION_ATTRIBUTE);
		String scriptText = DomUtils.getTextValue(element);
		if (!(StringUtils.hasText(scriptLocation) ^ StringUtils.hasText(scriptText))) {
			parserContext.getReaderContext().error("Either the 'location' attribute or inline script text must be provided, but not both.", element);
			return;
		}
		
		List<Element> variableElements = DomUtils.getChildElementsByTagName(element, "variable");
		String scriptVariableSourceName = element.getAttribute("script-variable-source");
		
		if (StringUtils.hasText(scriptText) && (variableElements.size() > 0 || StringUtils.hasText(scriptVariableSourceName))){
			parserContext.getReaderContext().error("Variable bindings or custom ScriptVariabelSource are not allowed when using inline groovy script. " +
					"Specify location of the script via 'location' attribute instead", element);
			return;
		}
		
		if (StringUtils.hasText(scriptVariableSourceName) && variableElements.size() > 0){
			parserContext.getReaderContext().error("'script-variable-source' and 'variable' sub-element are mutualy exclusive. Must use one or the other.", element);
			return;
		}
		
		if (StringUtils.hasText(scriptLocation)){
			builder.addConstructorArgValue(this.resolveScriptLocation(element, parserContext.getReaderContext(), scriptLocation));
		}
		else {
			builder.addConstructorArgValue(new StaticScriptSource(scriptText, "groovy.lang.Script"));
		}
				
		if (!StringUtils.hasText(scriptVariableSourceName)){
			BeanDefinitionBuilder scriptVariableSourceBuilder = 
				BeanDefinitionBuilder.genericBeanDefinition("org.springframework.integration.groovy.DefaultScriptVariableGenerator");
			
			ManagedMap<String, Object> variableMap = new ManagedMap<String, Object>();
			for (Element childElement : variableElements) {
				String variableName = childElement.getAttribute("name");
				String variableValue = childElement.getAttribute("value");
				String variableRef = childElement.getAttribute("ref");
				if (!(StringUtils.hasText(variableValue) ^ StringUtils.hasText(variableRef))) {
					parserContext.getReaderContext().error("Exactly one of the 'ref' attribute or 'value' attribute, " +
							" is required for element " +
							IntegrationNamespaceUtils.createElementDescription(element) + ".", element);
				}
				if (StringUtils.hasText(variableValue)){
					variableMap.put(variableName, variableValue);
				}
				else {
					variableMap.put(variableName, new RuntimeBeanReference(variableRef));
				}
			}
			if (!CollectionUtils.isEmpty(variableMap)){
				scriptVariableSourceBuilder.addConstructorArgValue(variableMap);
			}
			scriptVariableSourceName = 
				BeanDefinitionReaderUtils.registerWithGeneratedName(scriptVariableSourceBuilder.getBeanDefinition(), parserContext.getRegistry());
		}
		builder.addConstructorArgReference(scriptVariableSourceName);
	}

	private Object resolveScriptLocation(Element element, XmlReaderContext readerContext, String scriptLocation) {
		String refreshDelayText = element.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);			
		String beanClassName = "org.springframework.integration.groovy.config.RefreshableResourceScriptSource";
		BeanDefinitionBuilder resourceScriptSourceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(beanClassName);
		resourceScriptSourceBuilder.addConstructorArgValue(scriptLocation);
		if (StringUtils.hasText(refreshDelayText)) {
			resourceScriptSourceBuilder.addConstructorArgValue(refreshDelayText);
		}
		else {
			resourceScriptSourceBuilder.addConstructorArgValue(-1L);				
		}
		return resourceScriptSourceBuilder.getBeanDefinition();
	}

}
