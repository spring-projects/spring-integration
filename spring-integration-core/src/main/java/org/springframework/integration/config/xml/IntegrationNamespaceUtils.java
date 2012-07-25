/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.config.xml;

import static org.springframework.beans.factory.xml.AbstractBeanDefinitionParser.ID_ATTRIBUTE;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Shared utility methods for integration namespace parsers.
 *
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 */
public abstract class IntegrationNamespaceUtils {

	static final String BASE_PACKAGE = "org.springframework.integration";
	static final String REF_ATTRIBUTE = "ref";
	static final String METHOD_ATTRIBUTE = "method";
	static final String ORDER = "order";
	static final String EXPRESSION_ATTRIBUTE = "expression";
	public static final String HANDLER_ALIAS_SUFFIX = ".handler";
	public static final String REQUEST_HANDLER_ADVICE_CHAIN = "request-handler-advice-chain";

	/**
	 * Property name on ChannelInitializer used to configure the default max subscribers for
	 * unicast channels.
	 */
	public static String DEFAULT_MAX_UNICAST_SUBSCRIBERS_PROPERTY_NAME = "defaultMaxUnicastSubscribers";

	/**
	 * Property name on ChannelInitializer used to configure the default max subscribers for
	 * broadcast channels.
	 */
	public static String DEFAULT_MAX_BROADCAST_SUBSCRIBERS_PROPERTY_NAME = "defaultMaxBroadcastSubscribers";




	/**
	 * Configures the provided bean definition builder with a property value corresponding to the attribute whose name
	 * is provided if that attribute is defined in the given element.
	 *
	 * @param builder the bean definition builder to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be used to populate the property
	 * @param propertyName the name of the property to be populated
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder, Element element, String attributeName,
			String propertyName) {
		String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addPropertyValue(propertyName, new TypedStringValue(attributeValue));
		}
	}

	/**
	 * Configures the provided bean definition builder with a property value corresponding to the attribute whose name
	 * is provided if that attribute is defined in the given element.
	 *
	 * <p>
	 * The property name will be the camel-case equivalent of the lower case hyphen separated attribute (e.g. the
	 * "foo-bar" attribute would match the "fooBar" property).
	 *
	 * @see Conventions#attributeNameToPropertyName(String)
	 *
	 * @param builder the bean definition builder to be configured
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be set on the property
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder, Element element, String attributeName) {
		setValueIfAttributeDefined(builder, element, attributeName,
				Conventions.attributeNameToPropertyName(attributeName));
	}

	/**
	 * Configures the provided bean definition builder with a property reference to a bean. The bean reference is
	 * identified by the value from the attribute whose name is provided if that attribute is defined in the given
	 * element.
	 *
	 * @param builder the bean definition builder to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be used as a bean reference to populate the
	 * property
	 * @param propertyName the name of the property to be populated
	 */
	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder, Element element,
			String attributeName, String propertyName) {
		String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addPropertyReference(propertyName, attributeValue);
		}
	}

	/**
	 * Configures the provided bean definition builder with a property reference to a bean. The bean reference is
	 * identified by the value from the attribute whose name is provided if that attribute is defined in the given
	 * element.
	 *
	 * <p>
	 * The property name will be the camel-case equivalent of the lower case hyphen separated attribute (e.g. the
	 * "foo-bar" attribute would match the "fooBar" property).
	 *
	 * @see Conventions#attributeNameToPropertyName(String)
	 *
	 * @param builder the bean definition builder to be configured
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be used as a bean reference to populate the
	 * property
	 *
	 * @see Conventions#attributeNameToPropertyName(String)
	 */
	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder, Element element,
			String attributeName) {
		setReferenceIfAttributeDefined(builder, element, attributeName,
				Conventions.attributeNameToPropertyName(attributeName));
	}

	/**
	 * Provides a user friendly description of an element based on its node name and, if available, its "id" attribute
	 * value. This is useful for creating error messages from within bean definition parsers.
	 */
	public static String createElementDescription(Element element) {
		String elementId = "'" + element.getNodeName() + "'";
		String id = element.getAttribute("id");
		if (StringUtils.hasText(id)) {
			elementId += " with id='" + id + "'";
		}
		return elementId;
	}

	/**
	 * Parse a "poller" element to provide a reference for the target BeanDefinitionBuilder. If the poller element does
	 * not contain a "ref" attribute, this will create and register a PollerMetadata instance and then add it as a
	 * property reference of the target builder.
	 *
	 * @param pollerElement the "poller" element to parse
	 * @param targetBuilder the builder that expects the "trigger" property
	 * @param parserContext the parserContext for the target builder
	 */
	public static void configurePollerMetadata(Element pollerElement, BeanDefinitionBuilder targetBuilder,
			ParserContext parserContext) {
		if (pollerElement.hasAttribute("ref")) {
			int numberOfAttributes = pollerElement.getAttributes().getLength();
			if (numberOfAttributes != 1) {
				/*
				 * When importing the core namespace, e.g. into jdbc, we get a 'default="false"' attribute,
				 * even if not explicitly declared.
				 */
				if (!(numberOfAttributes == 2 &&
						pollerElement.hasAttribute("default") &&
						pollerElement.getAttribute("default").equals("false"))) {
					parserContext.getReaderContext().error(
							"A 'poller' element that provides a 'ref' must have no other attributes.", pollerElement);
				}
			}
			if (pollerElement.getChildNodes().getLength() != 0) {
				parserContext.getReaderContext().error(
						"A 'poller' element that provides a 'ref' must have no child elements.", pollerElement);
			}
			targetBuilder.addPropertyReference("pollerMetadata", pollerElement.getAttribute("ref"));
		} else {
			BeanDefinition beanDefinition = parserContext.getDelegate().parseCustomElement(pollerElement,
					targetBuilder.getBeanDefinition());
			if (beanDefinition == null) {
				parserContext.getReaderContext().error("BeanDefinition must not be null", pollerElement);
			}
			targetBuilder.addPropertyValue("pollerMetadata", beanDefinition);
		}
	}

	/**
	 * Get a text value from a named attribute if it exists, otherwise check for a nested element of the same name. If
	 * both are specified it is an error, but if neither is specified, just returns null.
	 *
	 * @param element a DOM node
	 * @param name the name of the property (attribute or child element)
	 * @param parserContext the current context
	 * @return the text from the attribite or element or null
	 */
	public static String getTextFromAttributeOrNestedElement(Element element, String name, ParserContext parserContext) {
		String attr = element.getAttribute(name);
		Element childElement = DomUtils.getChildElementByTagName(element, name);
		if (StringUtils.hasText(attr) && childElement != null) {
			parserContext.getReaderContext().error(
					"Either an attribute or a child element can be specified for " + name + " but not both", element);
			return null;
		}
		if (!StringUtils.hasText(attr) && childElement == null) {
			return null;
		}
		return StringUtils.hasText(attr) ? attr : childElement.getTextContent();
	}

	public static BeanComponentDefinition parseInnerHandlerDefinition(Element element, ParserContext parserContext) {
		// parses out the inner bean definition for concrete implementation if defined
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "bean");
		BeanComponentDefinition innerComponentDefinition = null;
		if (childElements != null && childElements.size() == 1) {
			Element beanElement = childElements.get(0);
			BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
			BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(beanElement);
			bdHolder = delegate.decorateBeanDefinitionIfRequired(beanElement, bdHolder);
			BeanDefinition inDef = bdHolder.getBeanDefinition();
			innerComponentDefinition = new BeanComponentDefinition(inDef, bdHolder.getBeanName());
		}
		String ref = element.getAttribute(REF_ATTRIBUTE);
		Assert.isTrue(!(StringUtils.hasText(ref) && innerComponentDefinition != null),
				"Ambiguous definition. Inner bean " + (innerComponentDefinition == null ? innerComponentDefinition
						: innerComponentDefinition.getBeanDefinition().getBeanClassName())
						+ " declaration and \"ref\" " + ref + " are not allowed together.");
		return innerComponentDefinition;
	}

	/**
	 * Utility method to configure HeaderMapper for Inbound and Outbound channel adapters/gateway
	 */
	public static void configureHeaderMapper(Element element, BeanDefinitionBuilder rootBuilder, ParserContext parserContext, Class<?> headerMapperClass, String replyHeaderValue){
		String defaultMappedReplyHeadersAttributeName = "mapped-reply-headers";
		if (!StringUtils.hasText(replyHeaderValue)){
			replyHeaderValue = defaultMappedReplyHeadersAttributeName;
		}
		boolean hasHeaderMapper = element.hasAttribute("header-mapper");
		boolean hasMappedRequestHeaders = element.hasAttribute("mapped-request-headers");
		boolean hasMappedReplyHeaders = element.hasAttribute(replyHeaderValue);

		if (hasHeaderMapper && (hasMappedRequestHeaders || hasMappedReplyHeaders)){
			parserContext.getReaderContext().error("The 'header-mapper' attribute is mutually exclusive with" +
					" 'mapped-request-headers' or 'mapped-reply-headers'. " +
					"You can only use one or the others", element);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(rootBuilder, element, "header-mapper");

		if (hasMappedRequestHeaders || hasMappedReplyHeaders){
			BeanDefinitionBuilder headerMapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(headerMapperClass);

			if (hasMappedRequestHeaders) {
				headerMapperBuilder.addPropertyValue("requestHeaderNames", element.getAttribute("mapped-request-headers"));
			}
			if (hasMappedReplyHeaders) {
				headerMapperBuilder.addPropertyValue("replyHeaderNames", element.getAttribute(replyHeaderValue));
			}

			rootBuilder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
	}

	/**
	 * Parse a "transactional" element and configure a {@link TransactionInterceptor}
	 * with "transactionManager" and other "transactionDefinition" properties.
	 * For example, this advisor will be applied on the Polling Task proxy
	 *
	 * @see AbstractPollingEndpoint
	 */
	public static BeanDefinition configureTransactionAttributes(Element txElement) {
		BeanDefinitionBuilder txDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultTransactionAttribute.class);
		txDefinitionBuilder.addPropertyValue("propagationBehaviorName", "PROPAGATION_" + txElement.getAttribute("propagation"));
		txDefinitionBuilder.addPropertyValue("isolationLevelName", "ISOLATION_" + txElement.getAttribute("isolation"));
		txDefinitionBuilder.addPropertyValue("timeout", txElement.getAttribute("timeout"));
		txDefinitionBuilder.addPropertyValue("readOnly", txElement.getAttribute("read-only"));
		BeanDefinitionBuilder attributeSourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(MatchAlwaysTransactionAttributeSource.class);
		attributeSourceBuilder.addPropertyValue("transactionAttribute", txDefinitionBuilder.getBeanDefinition());
		BeanDefinitionBuilder txInterceptorBuilder = BeanDefinitionBuilder.genericBeanDefinition(TransactionInterceptor.class);
		txInterceptorBuilder.addPropertyReference("transactionManager", txElement.getAttribute("transaction-manager"));
		txInterceptorBuilder.addPropertyValue("transactionAttributeSource", attributeSourceBuilder.getBeanDefinition());
		return txInterceptorBuilder.getBeanDefinition();
	}

	public static String[] generateAlias(Element element) {
		String[] handlerAlias = null;
		String id = element.getAttribute(ID_ATTRIBUTE);
		if (StringUtils.hasText(id)) {
			handlerAlias = new String[] {id + HANDLER_ALIAS_SUFFIX};
		}
		return handlerAlias;
	}

	@SuppressWarnings({ "rawtypes" })
	public static void configureAndSetAdviceChainIfPresent(Element adviceChainElement, Element txElement,
			BeanDefinitionBuilder parentBuilder, ParserContext parserContext) {
		ManagedList adviceChain = configureAdviceChain(adviceChainElement, txElement, parentBuilder, parserContext);
		if (adviceChain != null) {
			parentBuilder.addPropertyValue("adviceChain", adviceChain);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ManagedList configureAdviceChain(Element adviceChainElement, Element txElement,
			BeanDefinitionBuilder parentBuilder, ParserContext parserContext) {
		ManagedList adviceChain = null;
		// Schema validation ensures txElement and adviceChainElement are mutually exclusive
		if (txElement != null) {
			adviceChain = new ManagedList();
			adviceChain.add(IntegrationNamespaceUtils.configureTransactionAttributes(txElement));
		}
		if (adviceChainElement != null) {
			adviceChain = new ManagedList();
			NodeList childNodes = adviceChainElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element) child;
					String localName = child.getLocalName();
					if ("bean".equals(localName)) {
						BeanDefinitionHolder holder = parserContext.getDelegate().parseBeanDefinitionElement(
								childElement, parentBuilder.getBeanDefinition());
						parserContext.registerBeanComponent(new BeanComponentDefinition(holder));
						adviceChain.add(new RuntimeBeanReference(holder.getBeanName()));
					}
					else if ("ref".equals(localName)) {
						String ref = childElement.getAttribute("bean");
						adviceChain.add(new RuntimeBeanReference(ref));
					}
					else {
						BeanDefinition customBeanDefinition = parserContext.getDelegate().parseCustomElement(
								childElement, parentBuilder.getBeanDefinition());
						if (customBeanDefinition == null) {
							parserContext.getReaderContext().error(
									"failed to parse custom element '" + localName + "'", childElement);
						}
						adviceChain.add(customBeanDefinition);
					}
				}
			}
		}
		return adviceChain;
	}

	public static RootBeanDefinition createExpressionDefinitionFromValueOrExpression(String valueElementName,
			String expressionElementName, ParserContext parserContext, Element element, boolean oneRequired) {

	Assert.hasText(valueElementName, "'valueElementName' must not be empty");
	Assert.hasText(expressionElementName, "'expressionElementName' must no be empty");

	String valueElementValue = element.getAttribute(valueElementName);
	String expressionElementValue = element.getAttribute(expressionElementName);

	boolean hasAttributeValue = StringUtils.hasText(valueElementValue);
	boolean hasAttributeExpression = StringUtils.hasText(expressionElementValue);

	if (hasAttributeValue && hasAttributeExpression){
		parserContext.getReaderContext().error("Only one of '" + valueElementName + "' or '"
					+ expressionElementName + "' is allowed", element);
	}

	if (oneRequired && (!hasAttributeValue && !hasAttributeExpression)){
		parserContext.getReaderContext().error("One of '" + valueElementName + "' or '"
				+ expressionElementName + "' is required", element);
	}
	RootBeanDefinition expressionDef = null;
	if (hasAttributeValue) {
		expressionDef = new RootBeanDefinition(LiteralExpression.class);
	    expressionDef.getConstructorArgumentValues().addGenericArgumentValue(valueElementValue);
	}
	else if (hasAttributeExpression){
		expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
		expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expressionElementValue);
	}
	return expressionDef;
}
}
