/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.expression.SpelFunction;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Element;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class SpelFunctionParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return SpelFunction.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String className = element.getAttribute("class");
		String signature = element.getAttribute("method");

		Class clazz = null;
		try {
			clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException e) {
			parserContext.getReaderContext().error(e.getMessage(), element);
		}

		Method method = BeanUtils.resolveSignature(signature, clazz);

		if (method == null) {
			parserContext.getReaderContext().error(String.format("No declared method '%s' in class '%s'",
					signature, className), element);
			return;
		}
		if (!Modifier.isStatic(method.getModifiers())) {
			parserContext.getReaderContext().error("SpEL-function method has to be 'static'", element);
		}

		builder.addConstructorArgValue(method);
	}
}
