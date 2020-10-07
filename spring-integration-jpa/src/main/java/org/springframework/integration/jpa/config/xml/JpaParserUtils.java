/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.jpa.config.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Contains various utility methods for parsing JPA Adapter specific namespace
 * elements and generation the respective {@link BeanDefinition}s.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public final class JpaParserUtils {

	private static final LogAccessor LOGGER = new LogAccessor(JpaParserUtils.class);

	/**
	 * Create a new {@link BeanDefinitionBuilder} for the class {@link JpaExecutor}.
	 * Initialize the wrapped {@link JpaExecutor} with common properties.
	 * @param element Must not be null
	 * @param parserContext Must not be null
	 * @return The BeanDefinitionBuilder for the JpaExecutor
	 */
	public static BeanDefinitionBuilder getJpaExecutorBuilder(final Element element,
			final ParserContext parserContext) {

		Assert.notNull(element, "The provided element must not be null.");
		Assert.notNull(parserContext, "The provided parserContext must not be null.");

		final Object source = parserContext.extractSource(element);

		final BeanDefinitionBuilder jpaExecutorBuilder = BeanDefinitionBuilder.genericBeanDefinition(JpaExecutor.class);

		int attributeCount = 0;

		final String entityManagerRef = element.getAttribute("entity-manager");
		final String entityManagerFactoryRef = element.getAttribute("entity-manager-factory");
		final String jpaOperationsRef = element.getAttribute("jpa-operations");

		if (StringUtils.hasText(jpaOperationsRef)) {
			attributeCount++;
			jpaExecutorBuilder.addConstructorArgReference(jpaOperationsRef);
		}

		if (StringUtils.hasText(entityManagerRef)) {

			if (attributeCount > 0) {
				parserContext.getReaderContext().error("Exactly only one of the attributes 'entity-manager' or " +
						"'entity-manager-factory' or 'jpa-operations' must be be set.", source);
			}

			attributeCount++;
			jpaExecutorBuilder.addConstructorArgReference(entityManagerRef);
		}

		if (StringUtils.hasText(entityManagerFactoryRef)) {

			if (attributeCount > 0) {
				parserContext.getReaderContext().error("Exactly only one of the attributes 'entity-manager' or " +
						"'entity-manager-factory' or 'jpa-operations' must be be set.", source);
			}

			attributeCount++;
			jpaExecutorBuilder.addConstructorArgReference(entityManagerFactoryRef);
		}

		if (attributeCount == 0) {
			parserContext.getReaderContext().error("Exactly one of the attributes 'entity-manager' or " +
					"'entity-manager-factory' or 'jpa-operations' must be be set.", source);
		}

		final ManagedList<BeanDefinition> jpaParameterList = getJpaParameterBeanDefinitions(element, parserContext);

		if (!jpaParameterList.isEmpty()) {
			jpaExecutorBuilder.addPropertyValue("jpaParameters", jpaParameterList);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "entity-class");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "jpa-query");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "native-query");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "named-query");

		return jpaExecutorBuilder;

	}

	/**
	 * Create a new {@link BeanDefinitionBuilder} for the class {@link JpaExecutor}
	 * that is specific for JPA Outbound Gateways.
	 * Initializes the wrapped {@link JpaExecutor} with common properties.
	 * Delegates to {@link JpaParserUtils#getJpaExecutorBuilder(Element, ParserContext)}
	 * @param gatewayElement Must not be null
	 * @param parserContext Must not be null
	 * @return The BeanDefinitionBuilder for the JpaExecutor
	 */
	public static BeanDefinitionBuilder getOutboundGatewayJpaExecutorBuilder(final Element gatewayElement,
			final ParserContext parserContext) {

		final BeanDefinitionBuilder jpaExecutorBuilder = getJpaExecutorBuilder(gatewayElement, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement,
				"parameter-source-factory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement,
				"use-payload-as-parameter-source");

		return jpaExecutorBuilder;

	}

	/**
	 * Create a {@link ManagedList} of {@link BeanDefinition}s containing parsed
	 * JPA Parameters.
	 * @param jpaComponent Must not be null
	 * @param parserContext Must not be null
	 * @return {@link ManagedList} of {@link BeanDefinition}s
	 */
	public static ManagedList<BeanDefinition> getJpaParameterBeanDefinitions(
			Element jpaComponent, ParserContext parserContext) {

		Assert.notNull(jpaComponent, "The provided element must not be null.");
		Assert.notNull(parserContext, "The provided parserContext must not be null.");

		final ManagedList<BeanDefinition> parameterList = new ManagedList<>();

		final List<Element> parameterChildElements = DomUtils
				.getChildElementsByTagName(jpaComponent, "parameter");

		for (Element childElement : parameterChildElements) {
			BeanDefinitionBuilder parameterBuilder = BeanDefinitionBuilder.genericBeanDefinition(JpaParameter.class);

			String name = childElement.getAttribute("name");
			String expression = childElement.getAttribute("expression");
			String value = childElement.getAttribute("value");
			String type = childElement.getAttribute("type");

			if (StringUtils.hasText(name)) {
				parameterBuilder.addPropertyValue("name", name);
			}

			if (StringUtils.hasText(expression)) {
				parameterBuilder.addPropertyValue("expression", expression);
			}

			if (StringUtils.hasText(value)) {
				if (!StringUtils.hasText(type)) {
					LOGGER.info(() -> String.format("Type attribute not set for parameter '%s'. Defaulting to "
							+ "'java.lang.String'.", value));
					parameterBuilder.addPropertyValue("value", new TypedStringValue(value, String.class));
				}
				else {
					parameterBuilder.addPropertyValue("value", new TypedStringValue(value, type));
				}
			}

			parameterList.add(parameterBuilder.getBeanDefinition());
		}

		return parameterList;

	}

	private JpaParserUtils() {
	}

}
