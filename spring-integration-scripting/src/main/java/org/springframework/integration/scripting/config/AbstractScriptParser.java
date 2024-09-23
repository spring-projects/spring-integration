/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.scripting.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.scripting.DefaultScriptVariableGenerator;
import org.springframework.integration.scripting.RefreshableResourceScriptSource;
import org.springframework.scripting.support.StaticScriptSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author David Turanski
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 */
public abstract class AbstractScriptParser extends AbstractSingleBeanDefinitionParser {

	protected static final String LOCATION_ATTRIBUTE = "location";

	protected static final String REFRESH_CHECK_DELAY_ATTRIBUTE = "refresh-check-delay";

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String scriptLocation = element.getAttribute(LOCATION_ATTRIBUTE);
		String scriptText = DomUtils.getTextValue(element);
		if (StringUtils.hasText(scriptLocation) == StringUtils.hasText(scriptText)) {
			parserContext.getReaderContext().error(
					"Either the 'location' attribute or inline script text must be provided, but not both.", element);
			return;
		}

		List<Element> variableElements = DomUtils.getChildElementsByTagName(element, "variable");
		String scriptVariableGeneratorName = element.getAttribute("script-variable-generator");

		if (StringUtils.hasText(scriptVariableGeneratorName) && !variableElements.isEmpty()) {
			parserContext.getReaderContext().error(
					"'script-variable-generator' and 'variable' sub-elements are mutually exclusive.", element);
			return;
		}

		if (StringUtils.hasText(scriptLocation)) {
			builder.addConstructorArgValue(resolveScriptLocation(element, scriptLocation));
		}
		else {
			builder.addConstructorArgValue(new StaticScriptSource(scriptText));
		}

		BeanMetadataElement scriptVariableGeneratorDef;
		if (!StringUtils.hasText(scriptVariableGeneratorName)) {
			BeanDefinitionBuilder scriptVariableGeneratorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(DefaultScriptVariableGenerator.class);
			ManagedMap<String, Object> variableMap = buildVariablesMap(element, parserContext, variableElements);
			if (!CollectionUtils.isEmpty(variableMap)) {
				scriptVariableGeneratorBuilder.addConstructorArgValue(variableMap);
			}
			scriptVariableGeneratorDef = scriptVariableGeneratorBuilder.getBeanDefinition();
		}
		else {
			scriptVariableGeneratorDef = new RuntimeBeanReference(scriptVariableGeneratorName);
		}

		builder.addConstructorArgValue(scriptVariableGeneratorDef);
		postProcess(builder, element, parserContext);
	}

	/**
	 * Subclasses may override this no-op method to provide additional configuration.
	 *
	 * @param builder The builder.
	 * @param element The element.
	 * @param parserContext The parser context.
	 */
	protected void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
	}

	private Object resolveScriptLocation(Element element, String scriptLocation) {
		String refreshDelayText = element.getAttribute(REFRESH_CHECK_DELAY_ATTRIBUTE);
		String beanClassName = RefreshableResourceScriptSource.class.getName();
		BeanDefinitionBuilder resourceScriptSourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanClassName);
		resourceScriptSourceBuilder.addConstructorArgValue(scriptLocation);
		if (StringUtils.hasText(refreshDelayText)) {
			resourceScriptSourceBuilder.addConstructorArgValue(refreshDelayText);
		}
		else {
			resourceScriptSourceBuilder.addConstructorArgValue(-1L);
		}
		return resourceScriptSourceBuilder.getBeanDefinition();
	}

	private ManagedMap<String, Object> buildVariablesMap(final Element element, final ParserContext parserContext,
			List<Element> variableElements) {

		@SuppressWarnings("serial")
		ManagedMap<String, Object> variableMap = new ManagedMap<String, Object>() {

			@Override
			public Object put(String key, Object value) {
				if (this.containsKey(key)) {
					parserContext.getReaderContext().error("Duplicated variable: " + key, element);
				}
				return super.put(key, value);
			}

		};

		for (Element childElement : variableElements) {
			String variableName = childElement.getAttribute("name");
			String variableValue = childElement.getAttribute("value");
			String variableRef = childElement.getAttribute("ref");
			if (StringUtils.hasText(variableValue) == StringUtils.hasText(variableRef)) {
				parserContext.getReaderContext().error(
						"Exactly one of the 'ref' attribute or 'value' attribute, " + " is required for element "
								+ IntegrationNamespaceUtils.createElementDescription(element) + ".", element);
			}
			if (StringUtils.hasText(variableValue)) {
				variableMap.put(variableName, variableValue);
			}
			else {
				variableMap.put(variableName, new RuntimeBeanReference(variableRef));
			}
		}

		String variables = element.getAttribute("variables");
		if (StringUtils.hasText(variables)) {
			String[] variablePairs = StringUtils.commaDelimitedListToStringArray(variables);
			for (String variablePair : variablePairs) {
				String[] variableValue = variablePair.split("=");
				if (variableValue.length != 2) {
					parserContext.getReaderContext().error(
							"Variable declarations in the 'variable' attribute must have the "
									+ "form 'var=value'; found : '" + variablePair + "'", element);
				}
				String variable = variableValue[0].trim();
				String value = variableValue[1];
				if (variable.endsWith("-ref")) {
					variable = variable.substring(0, variable.indexOf("-ref"));
					variableMap.put(variable, new RuntimeBeanReference(value));
				}
				else {
					variableMap.put(variable, value);
				}
			}
		}

		return variableMap;
	}

}
