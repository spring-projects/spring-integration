/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.FixedSubscriberChannelBeanFactoryPostProcessor;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.transaction.TransactionHandleMessageAdvice;
import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
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
 */
public abstract class IntegrationNamespaceUtils {

	public static final String REF_ATTRIBUTE = "ref";

	public static final String METHOD_ATTRIBUTE = "method";

	public static final String ORDER = "order";

	public static final String EXPRESSION_ATTRIBUTE = "expression";

	public static final String REQUEST_HANDLER_ADVICE_CHAIN = "request-handler-advice-chain";

	public static final String AUTO_STARTUP = "auto-startup";

	public static final String PHASE = "phase";

	public static final String ROLE = "role";

	/**
	 * Configures the provided bean definition builder with a property value corresponding to the attribute whose name
	 * is provided if that attribute is defined in the given element.
	 * @param builder the bean definition builder to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be used to populate the property
	 * @param propertyName the name of the property to be populated
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder, Element element, String attributeName,
			String propertyName) {

		setValueIfAttributeDefined(builder, element, attributeName, propertyName, false);
	}

	/**
	 * Configures the provided bean definition builder with a property value corresponding to the attribute whose name
	 * is provided if that attribute is defined in the given element.
	 * <p> The property name will be the camel-case equivalent of the lower case hyphen separated attribute (e.g. the
	 * "foo-bar" attribute would match the "fooBar" property).
	 * @param builder the bean definition builder to be configured
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be set on the property
	 * @see Conventions#attributeNameToPropertyName(String)
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder, Element element,
			String attributeName) {

		setValueIfAttributeDefined(builder, element, attributeName, false);
	}

	/**
	 * Configures the provided bean definition builder with a property value corresponding to the attribute whose name
	 * is provided if that attribute is defined in the given element.
	 * @param builder the bean definition builder to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be used to populate the property
	 * @param propertyName the name of the property to be populated
	 * @param emptyStringAllowed - if true, the value is set, even if an empty String (""); if false, an empty
	 * String is treated as if the attribute wasn't provided.
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder, Element element, String attributeName,
			String propertyName, boolean emptyStringAllowed) {

		String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue) || (emptyStringAllowed && element.hasAttribute(attributeName))) {
			builder.addPropertyValue(propertyName, new TypedStringValue(attributeValue));
		}
	}

	/**
	 * Configures the provided bean definition builder with a property value corresponding to the attribute whose name
	 * is provided if that attribute is defined in the given element.
	 * <p> The property name will be the camel-case equivalent of the lower case hyphen separated attribute (e.g. the
	 * "foo-bar" attribute would match the "fooBar" property).
	 *
	 * @param builder the bean definition builder to be configured
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be set on the property
	 * @param emptyStringAllowed - if true, the value is set, even if an empty String (""); if false, an empty
	 * String is treated as if the attribute wasn't provided.
	 * @see Conventions#attributeNameToPropertyName(String)
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder, Element element, String attributeName,
			boolean emptyStringAllowed) {

		setValueIfAttributeDefined(builder, element, attributeName,
				Conventions.attributeNameToPropertyName(attributeName), emptyStringAllowed);
	}

	/**
	 * Configures the provided bean definition builder with a property reference to a bean. The bean reference is
	 * identified by the value from the attribute whose name is provided if that attribute is defined in the given
	 * element.
	 * @param builder the bean definition builder to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be used as a bean reference to populate the
	 * property
	 * @param propertyName the name of the property to be populated
	 */
	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder, Element element,
			String attributeName, String propertyName) {

		setReferenceIfAttributeDefined(builder, element, attributeName, propertyName, false);
	}

	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder, Element element,
			String attributeName, String propertyName, boolean emptyStringAllowed) {

		if (element.hasAttribute(attributeName)) {
			String attributeValue = element.getAttribute(attributeName);
			if (StringUtils.hasText(attributeValue)) {
				builder.addPropertyReference(propertyName, attributeValue);
			}
			else if (emptyStringAllowed) {
				builder.addPropertyValue(propertyName, null);
			}
		}
	}

	/**
	 * Configures the provided bean definition builder with a property reference to a bean. The bean reference is
	 * identified by the value from the attribute whose name is provided if that attribute is defined in the given
	 * element.
	 * <p> The property name will be the camel-case equivalent of the lower case hyphen separated attribute (e.g. the
	 * "foo-bar" attribute would match the "fooBar" property).
	 * @param builder the bean definition builder to be configured
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be used as a bean reference to populate the
	 * property
	 * @see Conventions#attributeNameToPropertyName(String)
	 */
	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder, Element element,
			String attributeName) {

		setReferenceIfAttributeDefined(builder, element, attributeName, false);
	}

	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder, Element element,
			String attributeName, boolean emptyStringAllowed) {

		setReferenceIfAttributeDefined(builder, element, attributeName,
				Conventions.attributeNameToPropertyName(attributeName), emptyStringAllowed);
	}

	/**
	 * Provides a user friendly description of an element based on its node name and, if available, its "id" attribute
	 * value. This is useful for creating error messages from within bean definition parsers.
	 * @param element The element.
	 * @return The description.
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
	 * @param pollerElement the "poller" element to parse
	 * @param targetBuilder the builder that expects the "trigger" property
	 * @param parserContext the parserContext for the target builder
	 */
	public static void configurePollerMetadata(Element pollerElement, BeanDefinitionBuilder targetBuilder,
			ParserContext parserContext) {

		if (pollerElement.hasAttribute(REF_ATTRIBUTE)) {
			int numberOfAttributes = pollerElement.getAttributes().getLength();
			/*
			 * When importing the core namespace, e.g. into jdbc, we get a 'default="false"' attribute,
			 * even if not explicitly declared.
			 */
			if (numberOfAttributes != 1 && !(numberOfAttributes == 2 &&
					pollerElement.hasAttribute("default") &&
					pollerElement.getAttribute("default").equals("false"))) {
				parserContext.getReaderContext().error(
						"A 'poller' element that provides a 'ref' must have no other attributes.", pollerElement);
			}
			if (pollerElement.getChildNodes().getLength() != 0) {
				parserContext.getReaderContext().error(
						"A 'poller' element that provides a 'ref' must have no child elements.", pollerElement);
			}
			targetBuilder.addPropertyReference("pollerMetadata", pollerElement.getAttribute(REF_ATTRIBUTE));
		}
		else {
			BeanDefinition beanDefinition = parserContext.getDelegate().parseCustomElement(pollerElement,
					targetBuilder.getBeanDefinition());
			if (beanDefinition == null) {
				parserContext.getReaderContext().error("BeanDefinition must not be null", pollerElement);
			}
			targetBuilder.addPropertyValue("pollerMetadata", beanDefinition);
		}
	}

	/**
	 * Get a text value from a named attribute if it exists, otherwise check for a nested element of the same name.
	 * If both are specified it is an error, but if neither is specified, just returns null.
	 * @param element a DOM node
	 * @param name the name of the property (attribute or child element)
	 * @param parserContext the current context
	 * @return the text from the attribute or element or null
	 */
	public static String getTextFromAttributeOrNestedElement(Element element, String name,
			ParserContext parserContext) {

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
		if (childElements.size() == 1) {
			Element beanElement = childElements.get(0);
			BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
			BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(beanElement);
			bdHolder = delegate.decorateBeanDefinitionIfRequired(beanElement, bdHolder); // NOSONAR never null
			BeanDefinition inDef = bdHolder.getBeanDefinition();
			innerComponentDefinition = new BeanComponentDefinition(inDef, bdHolder.getBeanName());
		}
		String ref = element.getAttribute(REF_ATTRIBUTE);
		if (StringUtils.hasText(ref) && innerComponentDefinition != null) {
			parserContext.getReaderContext().error(
					"Ambiguous definition. Inner bean "
							+ (innerComponentDefinition.getBeanDefinition().getBeanClassName())
							+ " declaration and \"ref\" " + ref + " are not allowed together on element "
							+ IntegrationNamespaceUtils.createElementDescription(element) + ".",
					parserContext.extractSource(element));
		}
		return innerComponentDefinition;
	}

	/**
	 * Utility method to configure a HeaderMapper for Inbound and Outbound channel adapters/gateway.
	 * @param element The element.
	 * @param rootBuilder The root builder.
	 * @param parserContext The parser context.
	 * @param headerMapperClass The header mapper class.
	 * @param replyHeaderValue The reply header value.
	 */
	public static void configureHeaderMapper(Element element, BeanDefinitionBuilder rootBuilder,
			ParserContext parserContext, Class<?> headerMapperClass, String replyHeaderValue) {

		configureHeaderMapper(element, rootBuilder, parserContext,
				BeanDefinitionBuilder.genericBeanDefinition(headerMapperClass), replyHeaderValue);
	}

	/**
	 * Utility method to configure a HeaderMapper for Inbound and Outbound channel adapters/gateway.
	 * @param element The element.
	 * @param rootBuilder The root builder.
	 * @param parserContext The parser context.
	 * @param headerMapperBuilder The header mapper builder.
	 * @param replyHeaderValueArg The reply header value.
	 */
	public static void configureHeaderMapper(Element element, BeanDefinitionBuilder rootBuilder,
			ParserContext parserContext, BeanDefinitionBuilder headerMapperBuilder,
			@Nullable String replyHeaderValueArg) {

		String defaultMappedReplyHeadersAttributeName = "mapped-reply-headers";
		String replyHeaderValue = replyHeaderValueArg;
		if (!StringUtils.hasText(replyHeaderValue)) {
			replyHeaderValue = defaultMappedReplyHeadersAttributeName;
		}
		boolean hasHeaderMapper = element.hasAttribute("header-mapper");
		boolean hasMappedRequestHeaders = element.hasAttribute("mapped-request-headers");
		boolean hasMappedReplyHeaders = element.hasAttribute(replyHeaderValue);

		if (hasHeaderMapper && (hasMappedRequestHeaders || hasMappedReplyHeaders)) {
			parserContext.getReaderContext().error("The 'header-mapper' attribute is mutually exclusive with" +
					" 'mapped-request-headers' or 'mapped-reply-headers'. " +
					"You can only use one or the others", element);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(rootBuilder, element, "header-mapper");

		if (hasMappedRequestHeaders || hasMappedReplyHeaders) {

			if (hasMappedRequestHeaders) {
				headerMapperBuilder.addPropertyValue("requestHeaderNames",
						element.getAttribute("mapped-request-headers"));
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
	 * For example, this advisor will be applied on the Polling Task proxy.
	 * @param txElement The transactional element.
	 * @return The bean definition.
	 * @see org.springframework.integration.endpoint.AbstractPollingEndpoint
	 */
	public static BeanDefinition configureTransactionAttributes(Element txElement) {
		return configureTransactionAttributes(txElement, false);
	}

	/**
	 * Parse a "transactional" element and configure a {@link TransactionInterceptor}
	 * or {@link TransactionHandleMessageAdvice}
	 * with "transactionManager" and other "transactionDefinition" properties.
	 * For example, this advisor will be applied on the Polling Task proxy.
	 * @param txElement The transactional element.
	 * @param handleMessageAdvice flag if to use {@link TransactionHandleMessageAdvice}
	 * or regular {@link TransactionInterceptor}
	 * @return The bean definition.
	 * @see org.springframework.integration.endpoint.AbstractPollingEndpoint
	 */
	public static BeanDefinition configureTransactionAttributes(Element txElement, boolean handleMessageAdvice) {
		BeanDefinition txDefinition = configureTransactionDefinition(txElement);
		BeanDefinitionBuilder attributeSourceBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(MatchAlwaysTransactionAttributeSource.class);
		attributeSourceBuilder.addPropertyValue("transactionAttribute", txDefinition);
		BeanDefinitionBuilder txInterceptorBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(handleMessageAdvice
						? TransactionHandleMessageAdvice.class
						: TransactionInterceptor.class);
		txInterceptorBuilder.addPropertyReference("transactionManager", txElement.getAttribute("transaction-manager"));
		txInterceptorBuilder.addPropertyValue("transactionAttributeSource", attributeSourceBuilder.getBeanDefinition());
		return txInterceptorBuilder.getBeanDefinition();
	}

	/**
	 * Parse attributes of "transactional" element and configure a {@link DefaultTransactionAttribute}
	 * with provided "transactionDefinition" properties.
	 * @param txElement The transactional element.
	 * @return The bean definition.
	 */
	public static BeanDefinition configureTransactionDefinition(Element txElement) {
		BeanDefinitionBuilder txDefinitionBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(DefaultTransactionAttribute.class);
		txDefinitionBuilder.addPropertyValue("propagationBehaviorName", "PROPAGATION_"
				+ txElement.getAttribute("propagation"));
		txDefinitionBuilder.addPropertyValue("isolationLevelName", "ISOLATION_" + txElement.getAttribute("isolation"));
		txDefinitionBuilder.addPropertyValue("timeout", txElement.getAttribute("timeout"));
		txDefinitionBuilder.addPropertyValue("readOnly", txElement.getAttribute("read-only"));
		return txDefinitionBuilder.getBeanDefinition();
	}

	public static String[] generateAlias(Element element) {
		String[] handlerAlias = null;
		String id = element.getAttribute(AbstractBeanDefinitionParser.ID_ATTRIBUTE);
		if (StringUtils.hasText(id)) {
			handlerAlias = new String[] { id + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX };
		}
		return handlerAlias;
	}

	public static void configureAndSetAdviceChainIfPresent(Element adviceChainElement, Element txElement,
			BeanDefinition parentBeanDefinition, ParserContext parserContext) {

		configureAndSetAdviceChainIfPresent(adviceChainElement, txElement, false, parentBeanDefinition, parserContext);
	}

	public static void configureAndSetAdviceChainIfPresent(Element adviceChainElement,
			Element txElement, boolean handleMessageAdvice, BeanDefinition parentBeanDefinition,
			ParserContext parserContext) {

		configureAndSetAdviceChainIfPresent(adviceChainElement, txElement, handleMessageAdvice,
				parentBeanDefinition, parserContext, "adviceChain");
	}

	public static void configureAndSetAdviceChainIfPresent(Element adviceChainElement, Element txElement,
			BeanDefinition parentBeanDefinition, ParserContext parserContext, String propertyName) {

		configureAndSetAdviceChainIfPresent(adviceChainElement, txElement, false, parentBeanDefinition,
				parserContext, propertyName);
	}

	@SuppressWarnings({ "rawtypes" })
	public static void configureAndSetAdviceChainIfPresent(Element adviceChainElement, Element txElement,
			boolean handleMessageAdvice, BeanDefinition parentBeanDefinition, ParserContext parserContext,
			String propertyName) {

		ManagedList adviceChain = configureAdviceChain(adviceChainElement, txElement, handleMessageAdvice,
				parentBeanDefinition, parserContext);
		if (!CollectionUtils.isEmpty(adviceChain)) {
			parentBeanDefinition.getPropertyValues().add(propertyName, adviceChain);
		}
	}

	@SuppressWarnings("rawtypes")
	public static ManagedList configureAdviceChain(Element adviceChainElement, Element txElement,
			BeanDefinition parentBeanDefinition, ParserContext parserContext) {

		return configureAdviceChain(adviceChainElement, txElement, false, parentBeanDefinition, parserContext);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ManagedList configureAdviceChain(Element adviceChainElement, Element txElement,
			boolean handleMessageAdvice, BeanDefinition parentBeanDefinition, ParserContext parserContext) {

		ManagedList adviceChain = new ManagedList();
		if (txElement != null) {
			adviceChain.add(configureTransactionAttributes(txElement, handleMessageAdvice));
		}
		if (adviceChainElement != null) {
			NodeList childNodes = adviceChainElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node child = childNodes.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					Element childElement = (Element) child;
					String localName = child.getLocalName();
					if ("bean".equals(localName)) {
						BeanDefinitionHolder holder = parserContext.getDelegate().parseBeanDefinitionElement(
								childElement, parentBeanDefinition);
						parserContext.registerBeanComponent(new BeanComponentDefinition(holder)); // NOSONAR never null
						adviceChain.add(new RuntimeBeanReference(holder.getBeanName()));
					}
					else if (REF_ATTRIBUTE.equals(localName)) {
						String ref = childElement.getAttribute("bean");
						adviceChain.add(new RuntimeBeanReference(ref));
					}
					else {
						BeanDefinition customBeanDefinition = parserContext.getDelegate().parseCustomElement(
								childElement, parentBeanDefinition);
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

	public static BeanDefinition createExpressionDefinitionFromValueOrExpression(String valueElementName,
			String expressionElementName, ParserContext parserContext, Element element, boolean oneRequired) {

		Assert.hasText(valueElementName, "'valueElementName' must not be empty");
		Assert.hasText(expressionElementName, "'expressionElementName' must not be empty");

		String valueElementValue = element.getAttribute(valueElementName);
		String expressionElementValue = element.getAttribute(expressionElementName);

		boolean hasAttributeValue = StringUtils.hasText(valueElementValue);
		boolean hasAttributeExpression = StringUtils.hasText(expressionElementValue);

		if (hasAttributeValue && hasAttributeExpression) {
			parserContext.getReaderContext().error("Only one of '" + valueElementName + "' or '"
					+ expressionElementName + "' is allowed", element);
		}

		if (oneRequired && (!hasAttributeValue && !hasAttributeExpression)) {
			parserContext.getReaderContext().error("One of '" + valueElementName + "' or '"
					+ expressionElementName + "' is required", element);
		}
		BeanDefinition expressionDef;
		if (hasAttributeValue) {
			expressionDef = new RootBeanDefinition(LiteralExpression.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(valueElementValue);
		}
		else {
			expressionDef = createExpressionDefIfAttributeDefined(expressionElementName, element);
		}
		return expressionDef;
	}

	public static BeanDefinition createExpressionDefIfAttributeDefined(String expressionElementName, Element element) {

		Assert.hasText(expressionElementName, "'expressionElementName' must no be empty");

		String expressionElementValue = element.getAttribute(expressionElementName);

		if (StringUtils.hasText(expressionElementValue)) {
			BeanDefinitionBuilder expressionDefBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionDefBuilder.addConstructorArgValue(expressionElementValue);
			return expressionDefBuilder.getRawBeanDefinition();
		}
		return null;
	}

	public static String createDirectChannel(Element element, ParserContext parserContext) {
		String channelId = element.getAttribute(AbstractBeanDefinitionParser.ID_ATTRIBUTE);
		if (!StringUtils.hasText(channelId)) {
			parserContext.getReaderContext().error("The channel-adapter's 'id' attribute is required when no 'channel' "
					+ "reference has been provided, because that 'id' would be used for the created channel.", element);
		}

		IntegrationConfigUtils.autoCreateDirectChannel(channelId, parserContext.getRegistry());

		return channelId;
	}

	@SuppressWarnings("unchecked")
	public static void checkAndConfigureFixedSubscriberChannel(Element element, ParserContext parserContext,
			String channelName, String handlerBeanName) {

		BeanDefinitionRegistry registry = parserContext.getRegistry();
		if (registry.containsBeanDefinition(channelName)) {
			BeanDefinition inputChannelDefinition = registry.getBeanDefinition(channelName);
			if (FixedSubscriberChannel.class.getName().equals(inputChannelDefinition.getBeanClassName())) {
				ConstructorArgumentValues constructorArgumentValues = inputChannelDefinition
						.getConstructorArgumentValues();
				if (constructorArgumentValues.isEmpty()) {
					constructorArgumentValues.addGenericArgumentValue(new RuntimeBeanReference(handlerBeanName));
				}
				else {
					parserContext.getReaderContext()
							.error("Only one subscriber is allowed for a FixedSubscriberChannel.", element);
				}
			}
		}
		else {
			BeanDefinition bfppd;
			if (!registry.containsBeanDefinition(
					IntegrationContextUtils.INTEGRATION_FIXED_SUBSCRIBER_CHANNEL_BPP_BEAN_NAME)) {

				bfppd = new RootBeanDefinition(FixedSubscriberChannelBeanFactoryPostProcessor.class);
				registry.registerBeanDefinition(
						IntegrationContextUtils.INTEGRATION_FIXED_SUBSCRIBER_CHANNEL_BPP_BEAN_NAME, bfppd);
			}
			else {
				bfppd = registry.getBeanDefinition(
						IntegrationContextUtils.INTEGRATION_FIXED_SUBSCRIBER_CHANNEL_BPP_BEAN_NAME);
			}
			ManagedMap<String, String> candidates;
			ValueHolder argumentValue = bfppd.getConstructorArgumentValues().getArgumentValue(0, Map.class);
			if (argumentValue == null) {
				candidates = new ManagedMap<>();
				bfppd.getConstructorArgumentValues().addIndexedArgumentValue(0, candidates);
			}
			else {
				candidates = (ManagedMap<String, String>) argumentValue.getValue();
			}
			candidates.put(handlerBeanName, channelName); // NOSONAR never null
		}
	}

	public static void injectPropertyWithAdapter(String beanRefAttribute, String methodRefAttribute,
			String expressionAttribute, String beanProperty, String adapterClass, Element element,
			BeanDefinitionBuilder builder, BeanMetadataElement processor, ParserContext parserContext) {

		BeanMetadataElement adapter = constructAdapter(beanRefAttribute, methodRefAttribute, expressionAttribute,
				adapterClass, element, processor, parserContext);

		if (adapter != null) {
			builder.addPropertyValue(beanProperty, adapter);
		}
	}

	public static void injectConstructorWithAdapter(String beanRefAttribute, String methodRefAttribute,
			String expressionAttribute, String adapterClass, Element element,
			BeanDefinitionBuilder builder, BeanMetadataElement processor, ParserContext parserContext) {

		BeanMetadataElement adapter = constructAdapter(beanRefAttribute, methodRefAttribute, expressionAttribute,
				adapterClass, element, processor, parserContext);

		if (adapter != null) {
			builder.addConstructorArgValue(adapter);
		}
	}

	private static BeanMetadataElement constructAdapter(String beanRefAttribute, String methodRefAttribute,
			String expressionAttribute, String adapterClass, Element element, BeanMetadataElement processor,
			ParserContext parserContext) {

		final String beanRef = element.getAttribute(beanRefAttribute);
		final String beanMethod = element.getAttribute(methodRefAttribute);
		final String expression = element.getAttribute(expressionAttribute);

		final boolean hasBeanRef = StringUtils.hasText(beanRef);
		final boolean hasExpression = StringUtils.hasText(expression);

		if (hasBeanRef && hasExpression) {
			parserContext.getReaderContext().error("Exactly one of the '" + beanRefAttribute + "' or '"
					+ expressionAttribute + "' attribute is allowed.", element);
		}

		BeanMetadataElement adapter = null;
		if (hasBeanRef) {
			adapter = createAdapter(new RuntimeBeanReference(beanRef), beanMethod, adapterClass);
		}
		else if (hasExpression) {
			BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationContextUtils.BASE_PACKAGE + ".aggregator.ExpressionEvaluating"
							+ adapterClass);
			adapterBuilder.addConstructorArgValue(expression);
			adapter = adapterBuilder.getBeanDefinition();
		}
		else if (processor != null) {
			adapter = createAdapter(processor, beanMethod, adapterClass);
		}

		return adapter;
	}

	private static BeanMetadataElement createAdapter(BeanMetadataElement ref, String method,
			String unqualifiedClassName) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(IntegrationContextUtils.BASE_PACKAGE + ".config." + unqualifiedClassName
						+ "FactoryBean");
		builder.addPropertyValue("target", ref);
		if (StringUtils.hasText(method)) {
			builder.addPropertyValue("methodName", method);
		}
		return builder.getBeanDefinition();
	}

}
