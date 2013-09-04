/*
 * Copyright 2013 the original author or authors.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.ClassUtils;

/**
 * Parser for the &lt;spel-function&gt; element.
 * Doesn't register a bean within application context, collects 'functions'
 * within {@link org.springframework.integration.config.SpelFunctionRegistrar} bean.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class SpelFunctionParser implements BeanDefinitionParser {

	private final Map<String, Method> functions = new LinkedHashMap<String, Method>();

	private volatile boolean initialized;

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {

		this.initializeSpelFunctionRegistrarIfNecessary(parserContext);

		String id = element.getAttribute("id");
		String className = element.getAttribute("class");
		String signature = element.getAttribute("method");

		Class<?> clazz = null;
		try {
			clazz = ClassUtils.forName(className, parserContext.getReaderContext().getBeanClassLoader());
		}
		catch (ClassNotFoundException e) {
			parserContext.getReaderContext().error(e.getMessage(), element);
		}

		Method method = BeanUtils.resolveSignature(signature, clazz);

		if (method == null) {
			parserContext.getReaderContext().error(String.format("No declared method '%s' in class '%s'",
					signature, className), element);
			return null;
		}
		if (!Modifier.isStatic(method.getModifiers())) {
			parserContext.getReaderContext().error("SpEL-function method has to be 'static'", element);
		}

		this.functions.put(id, method);

		return null;
	}

	private synchronized void initializeSpelFunctionRegistrarIfNecessary(ParserContext parserContext) {
		if (!this.initialized) {
			BeanDefinitionBuilder registrarBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".config.SpelFunctionRegistrar")
					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
					.addConstructorArgValue(this.functions);
			BeanDefinitionReaderUtils.registerWithGeneratedName(registrarBuilder.getBeanDefinition(),
					parserContext.getRegistry());
			this.initialized = true;
		}
	}

}
