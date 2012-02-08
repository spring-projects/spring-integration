/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.config.xml;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * The common method for generating the BeanDefinition for the common MessageHandler
 * is implemented in this class
 * 
 * @author Amol Nayak
 * @author Gunnar Hillert
 * 
 * @since 2.2
 * 
 */
public final class JpaParserUtils {

	private static final Log LOGGER = LogFactory.getLog(JpaParserUtils.class);
	
	/** Prevent instantiation. */
	private JpaParserUtils() {
		throw new AssertionError();
	}
	
	/**
	 * Create a new {@link BeanDefinitionBuilder} for the class {@link JpaExecutor}.
	 * Initialize the wrapped {@link JpaExecutor} with common properties.
	 * 
	 * @param element Must not be Null
	 * @param parserContext Must not be Null
	 * @return The BeanDefinitionBuilder for the JpaExecutor 
	 */
	public static BeanDefinitionBuilder getJpaExecutorBuilder(final Element element, 
			                                                  final ParserContext parserContext) {
        
		Assert.notNull(element,       "The provided element must not be Null.");
		Assert.notNull(parserContext, "The provided parserContext must not be Null.");
		
		final Object source = parserContext.extractSource(element);
		
		final BeanDefinitionBuilder jpaExecutorBuilder = BeanDefinitionBuilder.genericBeanDefinition(JpaExecutor.class);
		
		final String entityManagerRef        = element.getAttribute("entity-manager");
		final String entityManagerFactoryRef = element.getAttribute("entity-manager-factory");
		final String jpaOperationsRef        = element.getAttribute("jpa-operations");

		if (StringUtils.hasText(jpaOperationsRef)) {
			jpaExecutorBuilder.addConstructorArgReference(jpaOperationsRef);
		} else if (StringUtils.hasText(entityManagerRef)) {
			jpaExecutorBuilder.addConstructorArgReference(entityManagerRef);
		} else if (StringUtils.hasText(entityManagerFactoryRef)) {
			jpaExecutorBuilder.addConstructorArgReference(entityManagerFactoryRef);
		} else {
			parserContext.getReaderContext().error("Exactly one of the attributes 'entity-manager' or " +
					"'entity-manager-factory' or 'jpa-operations' must be be set.", source);
		}
		
		final ManagedList<BeanDefinition> jpaParameterList = JpaParserUtils.getProcedureParameterBeanDefinitions(element, parserContext);

		if (!jpaParameterList.isEmpty()) {
			jpaExecutorBuilder.addPropertyValue("jpaParameters", jpaParameterList);
		}
		
		return jpaExecutorBuilder;
	}
	
	/**
	 * @param storedProcComponent
	 * @param parserContext
	 */
	public static ManagedList<BeanDefinition> getProcedureParameterBeanDefinitions(
			Element jpaComponent, ParserContext parserContext) {

		final ManagedList<BeanDefinition> parameterList = new ManagedList<BeanDefinition>();

		final List<Element> parameterChildElements = DomUtils
				.getChildElementsByTagName(jpaComponent, "parameter");

		for (Element childElement : parameterChildElements) {

			final BeanDefinitionBuilder parameterBuilder = BeanDefinitionBuilder.genericBeanDefinition(JpaParameter.class);

			String name       = childElement.getAttribute("name");
			String expression = childElement.getAttribute("expression");
			String value      = childElement.getAttribute("value");
			String type       = childElement.getAttribute("type");

			if (StringUtils.hasText(name)) {
				parameterBuilder.addPropertyValue("name", name);
			} 

			if (StringUtils.hasText(expression)) {
				parameterBuilder.addPropertyValue("expression", expression);
			}

			if (StringUtils.hasText(value)) {

				if (!StringUtils.hasText(type)) {
					
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info(String
								.format("Type attribute not set for parameter '%s'. Defaulting to " 
									  + "'java.lang.String'.", value));
					}
					
					parameterBuilder.addPropertyValue("value",
							new TypedStringValue(value, String.class));

				} else {
					parameterBuilder.addPropertyValue("value",
							new TypedStringValue(value, type));
				}

			}

			parameterList.add(parameterBuilder.getBeanDefinition());
		}

		return parameterList;

	}

}
