/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.DynamicExpression;
import org.springframework.integration.transformer.HeaderEnricher;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.MessageProcessingHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.RoutingSlipHeaderValueMessageProcessor;
import org.springframework.integration.transformer.support.StaticHeaderValueMessageProcessor;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base support class for 'header-enricher' parsers.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public abstract class HeaderEnricherParserSupport extends AbstractTransformerParser {

	private static final String TYPE_ATTRIBUTE = "type";

	private static final Map<String, String[][]> cannedHeaderElementExpressions = new HashMap<>(); // NOSONAR lower case

	private final Map<String, String> elementToNameMap = new HashMap<>();

	private final Map<String, String> elementToTypeMap = new HashMap<>();

	static {
		cannedHeaderElementExpressions.put("header-channels-to-string", new String[][] {
				{"replyChannel", "@" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME
						+ ".channelToChannelName(headers.replyChannel, ####)"},
				{"errorChannel", "@" + IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME
						+ ".channelToChannelName(headers.errorChannel, ####)"},
		});
	}

	@Override
	protected final String getTransformerClassName() {
		return HeaderEnricher.class.getName();
	}

	protected final void addElementToHeaderMapping(String elementName, String headerName) {
		addElementToHeaderMapping(elementName, headerName, null);
	}

	protected final void addElementToHeaderMapping(String elementName, String headerName, @Nullable String headerType) {
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
				elementNode(element, headers, parserContext, node);
			}
		}
	}

	private void elementNode(Element element, ManagedMap<String, Object> headers, ParserContext parserContext,
			Node node) {

		String headerName = null;
		Element headerElement = (Element) node;
		String elementName = node.getLocalName();
		String headerType = null;
		String expression = null;
		String overwrite = headerElement.getAttribute("overwrite");
		if ("header".equals(elementName)) {
			headerName = headerElement.getAttribute(NAME_ATTRIBUTE);
		}
		else {
			headerName = this.elementToNameMap.get(elementName);
			headerType = this.elementToTypeMap.get(elementName);
			if (headerType != null && StringUtils.hasText(headerElement.getAttribute(TYPE_ATTRIBUTE))) {
				parserContext.getReaderContext().error("The " + elementName
						+ " header does not accept a 'type' attribute. The required type is ["
						+ headerType + "]", element);
			}
		}
		if (headerType == null) {
			headerType = headerElement.getAttribute(TYPE_ATTRIBUTE);
		}
		if (headerName == null) {
			String ttlExpression = headerElement.getAttribute("time-to-live-expression");
			if (cannedHeaderElementExpressions.containsKey(elementName)) {
				for (int j = 0; j < cannedHeaderElementExpressions.get(elementName).length; j++) {
					headerName = cannedHeaderElementExpressions.get(elementName)[j][0];
					expression = cannedHeaderElementExpressions.get(elementName)[j][1];
					if (StringUtils.hasText(ttlExpression)) {
						expression = expression.replace("####", ttlExpression);
					}
					else {
						expression = expression.replace(", ####", "");
					}
					overwrite = "true";
					addHeader(element, headers, parserContext, headerName, headerElement, headerType,
							expression, overwrite);
				}
			}
		}
		else {
			addHeader(element, headers, parserContext, headerName, headerElement, headerType, null, overwrite);
		}
	}

	@SuppressWarnings("NullAway")
	private void addHeader(Element element, ManagedMap<String, Object> headers, ParserContext parserContext, // NOSONAR complexity
			String headerName, Element headerElement, String headerType, @Nullable String expressionArg,
			String overwrite) {

		String value = headerElement.getAttribute("value");
		String ref = headerElement.getAttribute(REF_ATTRIBUTE);
		String method = headerElement.getAttribute(METHOD_ATTRIBUTE);
		String expression = expressionArg;
		if (expression == null) {
			expression = headerElement.getAttribute(EXPRESSION_ATTRIBUTE);
		}

		Element beanElement = null;
		Element scriptElement = null;
		Element expressionElement = null;

		List<Element> subElements = DomUtils.getChildElements(headerElement);
		if (!subElements.isEmpty()) {
			Element subElement = subElements.get(0);
			String subElementLocalName = subElement.getLocalName();
			if ("bean".equals(subElementLocalName)) {
				beanElement = subElement;
			}
			else if ("script".equals(subElementLocalName)) {
				scriptElement = subElement;
			}
			else if ("expression".equals(subElementLocalName)) {
				expressionElement = subElement;
			}
			if (beanElement == null && scriptElement == null && expressionElement == null) {
				parserContext.getReaderContext()
						.error("Only 'bean', 'script' or 'expression' can be defined as a sub-element", element);
			}
		}
		if (StringUtils.hasText(expression) && expressionElement != null) {
			parserContext.getReaderContext()
					.error("The 'expression' attribute and sub-element are mutually exclusive", element);
		}

		boolean isValue = StringUtils.hasText(value);
		boolean isRef = StringUtils.hasText(ref);
		boolean hasMethod = StringUtils.hasText(method);
		boolean isExpression = StringUtils.hasText(expression) || expressionElement != null;
		boolean isScript = scriptElement != null;

		BeanDefinition innerComponentDefinition = null;

		if (beanElement != null) {
			innerComponentDefinition = parserContext.getDelegate()
					.parseBeanDefinitionElement(beanElement)
					.getBeanDefinition(); // NOSONAR never null
		}
		else if (isScript) {
			innerComponentDefinition = parserContext.getDelegate().parseCustomElement(scriptElement);
		}

		BeanDefinitionBuilder valueProcessorBuilder = valueProcessor(element, parserContext, headerName, headerElement,
				headerType, overwrite, value, ref, method, expression, expressionElement, isValue, isRef, hasMethod,
				isExpression, isScript, innerComponentDefinition);
		headers.put(headerName, valueProcessorBuilder.getBeanDefinition());
	}

	private BeanDefinitionBuilder valueProcessor(Element element, ParserContext parserContext, String headerName,
			Element headerElement, String headerType, String overwrite, String value, String ref, String method,
			String expression, @Nullable Element expressionElement, boolean isValue, boolean isRef, boolean hasMethod,
			boolean isExpression, boolean isScript, @Nullable BeanDefinition innerComponentDefinition) {

		boolean isCustomBean = innerComponentDefinition != null;

		if (hasMethod && isScript) {
			parserContext.getReaderContext()
					.error("The 'method' attribute cannot be used when a 'script' sub-element is defined", element);
		}

		if (isValue == (isRef ^ (isExpression ^ isCustomBean))) {
			parserContext.getReaderContext().error(
					"Exactly one of the 'ref', 'value', 'expression' or inner bean is required.", element);
		}
		BeanDefinitionBuilder valueProcessorBuilder = null;
		if (isValue) {
			valueProcessorBuilder = value(element, parserContext, headerName, headerType, value, hasMethod);
		}
		else if (isExpression) {
			valueProcessorBuilder = expression(element, parserContext, headerType, expression, expressionElement,
					hasMethod);
		}
		else if (isCustomBean) {
			valueProcessorBuilder = innerComponentAndMethod(element, parserContext, headerElement, method, hasMethod,
					isScript, innerComponentDefinition);
		}
		else {
			valueProcessorBuilder = refAndMethod(element, parserContext, headerElement, ref, method, hasMethod);
		}
		if (StringUtils.hasText(overwrite)) {
			valueProcessorBuilder.addPropertyValue("overwrite", overwrite);
		}
		return valueProcessorBuilder;
	}

	private BeanDefinitionBuilder value(Element element, ParserContext parserContext, String headerName,
			String headerType, String value, boolean hasMethod) {

		BeanDefinitionBuilder valueProcessorBuilder;
		if (hasMethod) {
			parserContext.getReaderContext().error(
					"The 'method' attribute cannot be used with the 'value' attribute.", element);
		}
		if (IntegrationMessageHeaderAccessor.ROUTING_SLIP.equals(headerName)) {
			List<String> routingSlipPath = new ManagedList<>();
			routingSlipPath.addAll(Arrays.asList(StringUtils.tokenizeToStringArray(value, ";")));
			valueProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(RoutingSlipHeaderValueMessageProcessor.class)
							.addConstructorArgValue(routingSlipPath);
		}
		else {
			Object headerValue = value;

			if (StringUtils.hasText(headerType)) {
				TypedStringValue typedStringValue = new TypedStringValue(value);
				typedStringValue.setTargetTypeName(headerType);
				headerValue = typedStringValue;
			}
			valueProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(StaticHeaderValueMessageProcessor.class)
							.addConstructorArgValue(headerValue);
		}
		return valueProcessorBuilder;
	}

	private BeanDefinitionBuilder expression(Element element, ParserContext parserContext, String headerType,
			String expression, @Nullable Element expressionElement, boolean hasMethod) {

		BeanDefinitionBuilder valueProcessorBuilder;
		if (hasMethod) {
			parserContext.getReaderContext().error(
					"The 'method' attribute cannot be used with the 'expression' attribute.", element);
		}
		valueProcessorBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(ExpressionEvaluatingHeaderValueMessageProcessor.class);
		if (expressionElement != null) {
			BeanDefinitionBuilder dynamicExpressionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(DynamicExpression.class);
			dynamicExpressionBuilder.addConstructorArgValue(expressionElement.getAttribute("key"));
			dynamicExpressionBuilder.addConstructorArgReference(expressionElement.getAttribute("source"));
			valueProcessorBuilder.addConstructorArgValue(dynamicExpressionBuilder.getBeanDefinition());
		}
		else {
			valueProcessorBuilder.addConstructorArgValue(expression);
		}
		valueProcessorBuilder.addConstructorArgValue(headerType);
		return valueProcessorBuilder;
	}

	private BeanDefinitionBuilder innerComponentAndMethod(Element element, ParserContext parserContext,
			Element headerElement, String method, boolean hasMethod, boolean isScript,
			@Nullable BeanDefinition innerComponentDefinition) {

		BeanDefinitionBuilder valueProcessorBuilder;
		if (StringUtils.hasText(headerElement.getAttribute(TYPE_ATTRIBUTE))) {
			parserContext.getReaderContext().error(
					"The 'type' attribute cannot be used with an inner bean.", element);
		}
		if (hasMethod || isScript) {
			valueProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(MessageProcessingHeaderValueMessageProcessor.class)
							.addConstructorArgValue(innerComponentDefinition);
			if (hasMethod) {
				valueProcessorBuilder.addConstructorArgValue(method);
			}
		}
		else {
			valueProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(StaticHeaderValueMessageProcessor.class)
							.addConstructorArgValue(innerComponentDefinition);
		}
		return valueProcessorBuilder;
	}

	private BeanDefinitionBuilder refAndMethod(Element element, ParserContext parserContext, Element headerElement,
			String ref, String method, boolean hasMethod) {

		BeanDefinitionBuilder valueProcessorBuilder;
		if (StringUtils.hasText(headerElement.getAttribute(TYPE_ATTRIBUTE))) {
			parserContext.getReaderContext().error(
					"The 'type' attribute cannot be used with the 'ref' attribute.", element);
		}
		if (hasMethod) {
			valueProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(MessageProcessingHeaderValueMessageProcessor.class)
							.addConstructorArgReference(ref)
							.addConstructorArgValue(method);
		}
		else {
			valueProcessorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(StaticHeaderValueMessageProcessor.class)
							.addConstructorArgReference(ref);
		}
		return valueProcessorBuilder;
	}

	/**
	 * Subclasses may override this method to provide any additional processing.
	 * @param builder The builder.
	 * @param element The element.
	 * @param parserContext The parser context.
	 */
	protected void postProcessHeaderEnricher(BeanDefinitionBuilder builder, Element element,
			ParserContext parserContext) {
	}

}
