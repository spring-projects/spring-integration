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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.AnnotationConstants;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.gateway.GatewayMethodMetadata;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;gateway/&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class GatewayParser implements BeanDefinitionParser {

	private static final String PROXY_DEFAULT_METHODS_ATTR = "proxyDefaultMethods";

	private static final String ASYNC_EXECUTOR_ATTR = "asyncExecutor";

	private static final String MAPPER_ATTR = "mapper";

	@Override
	public @Nullable BeanDefinition parse(final Element element, ParserContext parserContext) {
		boolean isNested = parserContext.isNested();

		final Map<String, @Nullable Object> gatewayAttributes = new HashMap<>();
		gatewayAttributes.put(AbstractBeanDefinitionParser.NAME_ATTRIBUTE,
				element.getAttribute(AbstractBeanDefinitionParser.ID_ATTRIBUTE));
		gatewayAttributes.put("defaultPayloadExpression", element.getAttribute("default-payload-expression"));
		gatewayAttributes.put("defaultRequestChannel",
				element.getAttribute(isNested ? "request-channel" : "default-request-channel"));
		gatewayAttributes.put("defaultReplyChannel",
				element.getAttribute(isNested ? "reply-channel" : "default-reply-channel"));
		gatewayAttributes.put("errorChannel", element.getAttribute("error-channel"));

		String asyncExecutor = element.getAttribute("async-executor");
		if (!element.hasAttribute("async-executor") || StringUtils.hasLength(asyncExecutor)) {
			gatewayAttributes.put(ASYNC_EXECUTOR_ATTR, asyncExecutor);
		}
		else {
			gatewayAttributes.put(ASYNC_EXECUTOR_ATTR, null);
		}

		gatewayAttributes.put(MAPPER_ATTR, element.getAttribute(MAPPER_ATTR));
		gatewayAttributes.put("defaultReplyTimeout",
				element.getAttribute(isNested ? "reply-timeout" : "default-reply-timeout"));
		gatewayAttributes.put("defaultRequestTimeout",
				element.getAttribute(isNested ? "request-timeout" : "default-request-timeout"));

		headers(element, gatewayAttributes);

		methods(element, parserContext, gatewayAttributes);

		gatewayAttributes.put("serviceInterface", element.getAttribute("service-interface"));

		gatewayAttributes.put("proxyDefaultMethods", element.getAttribute("proxy-default-methods"));

		BeanDefinitionHolder gatewayHolder = buildBeanDefinition(gatewayAttributes, parserContext);
		if (isNested) {
			return gatewayHolder.getBeanDefinition();
		}
		else {
			BeanDefinitionReaderUtils.registerBeanDefinition(gatewayHolder, parserContext.getRegistry());
			return null;
		}
	}

	private static void headers(Element element, Map<String, @Nullable Object> gatewayAttributes) {
		List<Element> headerElements = DomUtils.getChildElementsByTagName(element, "default-header");
		if (!CollectionUtils.isEmpty(headerElements)) {
			List<Map<String, Object>> headers = new ArrayList<>(headerElements.size());
			for (Element e : headerElements) {
				Map<String, Object> header = new HashMap<>();
				header.put(AbstractBeanDefinitionParser.NAME_ATTRIBUTE,
						e.getAttribute(AbstractBeanDefinitionParser.NAME_ATTRIBUTE));
				header.put(IntegrationNamespaceUtils.VALUE_ATTRIBUTE,
						e.getAttribute(IntegrationNamespaceUtils.VALUE_ATTRIBUTE));
				header.put(IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE,
						e.getAttribute(IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE));
				headers.add(header);
			}
			gatewayAttributes.put("defaultHeaders", headers.toArray(new Map<?, ?>[0]));
		}
	}

	private static void methods(Element element, ParserContext parserContext,
			final Map<String, @Nullable Object> gatewayAttributes) {

		List<Element> methodElements = DomUtils.getChildElementsByTagName(element, "method");
		if (!CollectionUtils.isEmpty(methodElements)) {
			Map<String, BeanDefinition> methodMetadataMap = new ManagedMap<>();
			for (Element methodElement : methodElements) {
				String methodName = methodElement.getAttribute(AbstractBeanDefinitionParser.NAME_ATTRIBUTE);
				BeanDefinitionBuilder methodMetadataBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						GatewayMethodMetadata.class);
				methodMetadataBuilder.addPropertyValue("requestChannelName",
						methodElement.getAttribute("request-channel"));
				methodMetadataBuilder.addPropertyValue("replyChannelName", methodElement.getAttribute("reply-channel"));
				methodMetadataBuilder.addPropertyValue("requestTimeout", methodElement.getAttribute("request-timeout"));
				methodMetadataBuilder.addPropertyValue("replyTimeout", methodElement.getAttribute("reply-timeout"));

				boolean hasMapper = StringUtils.hasText(element.getAttribute(MAPPER_ATTR));
				String payloadExpression = methodElement.getAttribute("payload-expression");
				Assert.state(!hasMapper || !StringUtils.hasText(payloadExpression),
						"'payload-expression' is not allowed when a 'mapper' is provided");

				if (StringUtils.hasText(payloadExpression)) {
					methodMetadataBuilder.addPropertyValue("payloadExpression",
							BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
									.addConstructorArgValue(payloadExpression)
									.getBeanDefinition());
				}

				List<Element> invocationHeaders = DomUtils.getChildElementsByTagName(methodElement, "header");
				if (!CollectionUtils.isEmpty(invocationHeaders)) {
					Assert.state(!hasMapper, "header elements are not allowed when a 'mapper' is provided");

					Map<String, @Nullable Object> headerExpressions = new ManagedMap<>();
					for (Element headerElement : invocationHeaders) {
						BeanDefinition expressionDef = IntegrationNamespaceUtils
								.createExpressionDefinitionFromValueOrExpression(
										IntegrationNamespaceUtils.VALUE_ATTRIBUTE, "expression", parserContext,
										headerElement, true);

						headerExpressions.put(headerElement.getAttribute(AbstractBeanDefinitionParser.NAME_ATTRIBUTE),
								expressionDef);
					}
					methodMetadataBuilder.addPropertyValue("headerExpressions", headerExpressions);
				}
				methodMetadataMap.put(methodName, methodMetadataBuilder.getBeanDefinition());
			}

			gatewayAttributes.put("methods", methodMetadataMap);
		}
	}

	private static BeanDefinitionHolder buildBeanDefinition(Map<String, @Nullable Object> gatewayAttributes, // NOSONAR complexity
			ParserContext parserContext) {

		BeanDefinitionRegistry registry = parserContext.getRegistry();
		String defaultPayloadExpression = (String) gatewayAttributes.get("defaultPayloadExpression");

		@SuppressWarnings("unchecked")
		Map<String, Object>[] defaultHeaders = (Map<String, Object>[]) gatewayAttributes.get("defaultHeaders");

		String defaultRequestChannel = (String) gatewayAttributes.get("defaultRequestChannel");
		String defaultReplyChannel = (String) gatewayAttributes.get("defaultReplyChannel");
		String errorChannel = (String) gatewayAttributes.get("errorChannel");
		String asyncExecutor = (String) gatewayAttributes.get(ASYNC_EXECUTOR_ATTR);
		String mapper = (String) gatewayAttributes.get(MAPPER_ATTR);
		String proxyDefaultMethods = (String) gatewayAttributes.get(PROXY_DEFAULT_METHODS_ATTR);

		boolean hasMapper = StringUtils.hasText(mapper);
		boolean hasDefaultPayloadExpression = StringUtils.hasText(defaultPayloadExpression);
		Assert.state(!hasMapper || !hasDefaultPayloadExpression,
				"'defaultPayloadExpression' is not allowed when a 'mapper' is provided");

		boolean hasDefaultHeaders = !ObjectUtils.isEmpty(defaultHeaders);
		Assert.state(!hasMapper || !hasDefaultHeaders,
				"'defaultHeaders' are not allowed when a 'mapper' is provided");

		ConfigurableBeanFactory beanFactory = obtainBeanFactory(registry);
		Class<?> serviceInterface = getServiceInterface((String) Objects.requireNonNull(gatewayAttributes.get("serviceInterface")), beanFactory);

		BeanDefinitionBuilder gatewayProxyBuilder =
				BeanDefinitionBuilder.rootBeanDefinition(GatewayProxyFactoryBean.class)
						.addConstructorArgValue(serviceInterface);

		if (hasDefaultHeaders || hasDefaultPayloadExpression) {
			BeanDefinition methodMetadata = getMethodMetadataBeanDefinition(defaultPayloadExpression, defaultHeaders);

			gatewayProxyBuilder.addPropertyValue("globalMethodMetadata", methodMetadata);
		}

		if (StringUtils.hasText(defaultRequestChannel)) {
			gatewayProxyBuilder.addPropertyValue("defaultRequestChannelName", defaultRequestChannel);
		}
		if (StringUtils.hasText(defaultReplyChannel)) {
			gatewayProxyBuilder.addPropertyValue("defaultReplyChannelName", defaultReplyChannel);
		}
		if (StringUtils.hasText(errorChannel)) {
			gatewayProxyBuilder.addPropertyValue("errorChannelName", errorChannel);
		}
		if (asyncExecutor == null || AnnotationConstants.NULL.equals(asyncExecutor)) {
			gatewayProxyBuilder.addPropertyValue(ASYNC_EXECUTOR_ATTR, null);
		}
		else if (StringUtils.hasText(asyncExecutor)) {
			gatewayProxyBuilder.addPropertyReference(ASYNC_EXECUTOR_ATTR, asyncExecutor);
		}
		if (StringUtils.hasText(mapper)) {
			gatewayProxyBuilder.addPropertyReference(MAPPER_ATTR, mapper);
		}
		if (StringUtils.hasText(proxyDefaultMethods)) {
			gatewayProxyBuilder.addPropertyValue(PROXY_DEFAULT_METHODS_ATTR, proxyDefaultMethods);
		}

		gatewayProxyBuilder.addPropertyValue("defaultRequestTimeoutExpressionString",
				gatewayAttributes.get("defaultRequestTimeout"));
		gatewayProxyBuilder.addPropertyValue("defaultReplyTimeoutExpressionString",
				gatewayAttributes.get("defaultReplyTimeout"));
		gatewayProxyBuilder.addPropertyValue("methodMetadataMap", gatewayAttributes.get("methods"));

		String id = (String) gatewayAttributes.get(AbstractBeanDefinitionParser.NAME_ATTRIBUTE);
		if (!StringUtils.hasText(id)) {
			BeanNameGenerator beanNameGenerator =
					IntegrationConfigUtils.annotationBeanNameGenerator(registry);
			id = beanNameGenerator.generateBeanName(new AnnotatedGenericBeanDefinition(serviceInterface), registry);
		}

		RootBeanDefinition beanDefinition = (RootBeanDefinition) gatewayProxyBuilder.getBeanDefinition();
		beanDefinition.setTargetType(
				ResolvableType.forClassWithGenerics(GatewayProxyFactoryBean.class, serviceInterface));
		return new BeanDefinitionHolder(beanDefinition, id);
	}

	private static BeanDefinition getMethodMetadataBeanDefinition(@Nullable String defaultPayloadExpression,
			@Nullable Map<String, Object> @Nullable [] defaultHeaders) {

		BeanDefinitionBuilder methodMetadataBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(GatewayMethodMetadata.class);

		if (StringUtils.hasText(defaultPayloadExpression)) {
			methodMetadataBuilder.addPropertyValue("payloadExpression",
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
							.addConstructorArgValue(defaultPayloadExpression)
							.getBeanDefinition());
		}

		if (!ObjectUtils.isEmpty(defaultHeaders)) {
			Map<String, Object> headerExpressions = new ManagedMap<>();
			for (Map<String, Object> header : defaultHeaders) {
				String headerValue = (String) Objects.requireNonNull(header).get(IntegrationNamespaceUtils.VALUE_ATTRIBUTE);
				String headerExpression = (String) header.get(IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE);
				boolean hasValue = StringUtils.hasText(headerValue);

				if (hasValue == StringUtils.hasText(headerExpression)) {
					throw new BeanDefinitionStoreException("exactly one of 'value' or 'expression' " +
							"is required on a gateway's header.");
				}

				BeanDefinition expressionDef =
						new RootBeanDefinition(hasValue ? LiteralExpression.class : ExpressionFactoryBean.class);
				expressionDef.getConstructorArgumentValues()
						.addGenericArgumentValue(hasValue ? headerValue : headerExpression);

				headerExpressions.put((String) header.get(AbstractBeanDefinitionParser.NAME_ATTRIBUTE), expressionDef);
			}
			methodMetadataBuilder.addPropertyValue("headerExpressions", headerExpressions);
		}
		return methodMetadataBuilder.getBeanDefinition();
	}

	private static ConfigurableBeanFactory obtainBeanFactory(BeanDefinitionRegistry registry) {
		if (registry instanceof ConfigurableBeanFactory) {
			return (ConfigurableBeanFactory) registry;
		}
		else if (registry instanceof ConfigurableApplicationContext) {
			return ((ConfigurableApplicationContext) registry).getBeanFactory();
		}
		throw new IllegalArgumentException("The provided 'BeanDefinitionRegistry' must be an instance " +
				"of 'ConfigurableBeanFactory' or 'ConfigurableApplicationContext', but given is: "
				+ registry.getClass());
	}

	private static Class<?> getServiceInterface(String serviceInterface, ConfigurableBeanFactory beanFactory) {
		String actualServiceInterface = beanFactory.resolveEmbeddedValue(serviceInterface);
		if (!StringUtils.hasText(actualServiceInterface)) {
			return RequestReplyExchanger.class;
		}
		try {
			return ClassUtils.forName(actualServiceInterface, beanFactory.getBeanClassLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new BeanDefinitionStoreException("Cannot parse class for service interface", ex);
		}
	}

}
