/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base support class for 'header-enricher' parsers.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public abstract class HeaderEnricherParserSupport extends AbstractTransformerParser {

	private final Map<String, String> elementToNameMap = new HashMap<String, String>();

	private final Map<String, Class<?>> elementToTypeMap = new HashMap<String, Class<?>>();


	@Override
	protected final String getTransformerClassName() {
		return IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher";
	}

	protected final void addElementToHeaderMapping(String elementName, String headerName) {
		this.addElementToHeaderMapping(elementName, headerName, null);
	}

	protected final void addElementToHeaderMapping(String elementName, String headerName, Class<?> headerType) {
		this.elementToNameMap.put(elementName, headerName);
		if (headerType != null) {
			this.elementToTypeMap.put(elementName, headerType);
		}
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		ManagedMap headers = new ManagedMap();
		this.processHeaders(element, headers, parserContext);
		builder.addConstructorArgValue(headers);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-overwrite");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "should-skip-nulls");
		this.postProcessHeaderEnricher(builder, element, parserContext);
	}

	protected void processHeaders(Element element, ManagedMap<String, Object> headers, ParserContext parserContext) {
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String headerName = null;
				Element headerElement = (Element) node;
				String elementName = node.getLocalName();
				Class<?> headerType = null;
				if ("header".equals(elementName)) {
					headerName = headerElement.getAttribute("name");
				}
				else {
					headerName = elementToNameMap.get(elementName);
					headerType = elementToTypeMap.get(elementName);
					if (headerType != null && StringUtils.hasText(headerElement.getAttribute("type"))) {
						parserContext.getReaderContext().error("The " + elementName
								+ " header does not accept a 'type' attribute. The required type is ["
								+ headerType.getName() + "]", element);
					}
				}
				if (headerType == null) {
					String headerTypeName = headerElement.getAttribute("type");
					if (StringUtils.hasText(headerTypeName)) {
						ClassLoader classLoader = parserContext.getReaderContext().getBeanClassLoader();
						if (classLoader == null) {
							classLoader = getClass().getClassLoader();
						}
						try {
							headerType = ClassUtils.forName(headerTypeName, classLoader);
						}
						catch (Exception e) {
							parserContext.getReaderContext().error("unable to resolve type [" +
									headerTypeName + "] for header '" + headerName + "'", element, e);
						}
					}
				}
				if (headerName != null) {
					String value = headerElement.getAttribute("value");
					String ref = headerElement.getAttribute("ref");
					String method = headerElement.getAttribute("method");
					String expression = headerElement.getAttribute("expression");
					Element beanElement = DomUtils.getChildElementByTagName(headerElement, "bean");
					Element scriptElement = DomUtils.getChildElementByTagName(headerElement, "script");
					Element expressionElement = DomUtils.getChildElementByTagName(headerElement, "expression");

					BeanDefinition innerComponentDefinition = null;

					if (StringUtils.hasText(expression) && expressionElement != null) {
						parserContext.getReaderContext().error("The 'expression' attribute and sub-element are mutually exclusive", element);
					}


					boolean isValue = StringUtils.hasText(value);
					boolean isRef = StringUtils.hasText(ref);
					boolean hasMethod = StringUtils.hasText(method);
					boolean isExpression = StringUtils.hasText(expression) || expressionElement != null;
					boolean isScript = scriptElement != null;

					if (beanElement != null) {
						innerComponentDefinition = parserContext.getDelegate().parseBeanDefinitionElement(beanElement).getBeanDefinition();
					}
					else if (isScript) {
						innerComponentDefinition = parserContext.getDelegate().parseCustomElement(scriptElement);
					}

					boolean isCustomBean = innerComponentDefinition != null;

					if (hasMethod && isScript) {
						parserContext.getReaderContext().error("The 'method' attribute can't be used when defined 'script' sub-element", element);
					}


					if (!(isValue ^ (isRef ^ (isExpression ^ isCustomBean)))) {
						parserContext.getReaderContext().error(
								"Exactly one of the 'ref', 'value', 'expression' or inner bean is required.", element);
					}
					BeanDefinitionBuilder valueProcessorBuilder = null;
					if (isValue) {
						if (hasMethod) {
							parserContext.getReaderContext().error(
									"The 'method' attribute cannot be used with the 'value' attribute.", element);
						}
						Object headerValue = (headerType != null) ?
								new TypedStringValue(value, headerType) : value;
						valueProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
								IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher$StaticHeaderValueMessageProcessor");
						valueProcessorBuilder.addConstructorArgValue(headerValue);
					}
					else if (isExpression) {
						if (hasMethod) {
							parserContext.getReaderContext().error(
									"The 'method' attribute cannot be used with the 'expression' attribute.", element);
						}
						valueProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
								IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher$ExpressionEvaluatingHeaderValueMessageProcessor");
						if (expressionElement != null) {
							BeanDefinitionBuilder dynamicExpressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(
									"org.springframework.integration.expression.DynamicExpression");
							dynamicExpressionBuilder.addConstructorArgValue(expressionElement.getAttribute("key"));
							dynamicExpressionBuilder.addConstructorArgReference(expressionElement.getAttribute("source"));
							valueProcessorBuilder.addConstructorArgValue(dynamicExpressionBuilder.getBeanDefinition());
						}
						else {
							valueProcessorBuilder.addConstructorArgValue(expression);
						}
						valueProcessorBuilder.addConstructorArgValue(headerType);
					}
					else if (isCustomBean) {
						if (StringUtils.hasText(headerElement.getAttribute("type"))) {
							parserContext.getReaderContext().error(
									"The 'type' attribute cannot be used with the inner beans.", element);
						}
						if (hasMethod || isScript) {
							valueProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
									IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher$MethodInvokingHeaderValueMessageProcessor");
							valueProcessorBuilder.addConstructorArgValue(innerComponentDefinition);
							valueProcessorBuilder.addConstructorArgValue(hasMethod ? method : null);
						}
						else {
							valueProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
									IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher$StaticHeaderValueMessageProcessor");
							valueProcessorBuilder.addConstructorArgValue(innerComponentDefinition);
						}
					}
					else {
						if (StringUtils.hasText(headerElement.getAttribute("type"))) {
							parserContext.getReaderContext().error(
									"The 'type' attribute cannot be used with the 'ref' attribute.", element);
						}
						if (hasMethod) {
							valueProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
									IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher$MethodInvokingHeaderValueMessageProcessor");
							valueProcessorBuilder.addConstructorArgReference(ref);
							valueProcessorBuilder.addConstructorArgValue(method);
						}
						else {
							valueProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
									IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher$StaticHeaderValueMessageProcessor");
							valueProcessorBuilder.addConstructorArgReference(ref);
						}
					}
					IntegrationNamespaceUtils.setValueIfAttributeDefined(valueProcessorBuilder, headerElement, "overwrite");
					headers.put(headerName, valueProcessorBuilder.getBeanDefinition());
				}
			}
		}
	}

	/**
	 * Subclasses may override this method to provide any additional processing.
	 */
	protected void postProcessHeaderEnricher(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
	}

}
