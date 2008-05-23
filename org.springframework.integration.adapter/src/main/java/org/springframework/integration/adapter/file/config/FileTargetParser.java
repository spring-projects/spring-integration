/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.file.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.adapter.file.FileTarget;
import org.springframework.integration.adapter.file.SimpleFileMessageMapper;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;file-target/&gt; element.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FileTargetParser extends AbstractSimpleBeanDefinitionParser {

	private static final String NAME_GENERATOR_PROPERTY = "fileNameGenerator";

	public static final String DIRECTORY_ATTRIBUTE = "directory";

	public static final String FILE_NAME_GENERATOR_ATTRIBUTE = "name-generator";

	
	@Override
	protected Class<?> getBeanClass(Element element) {
		return FileTarget.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !(DIRECTORY_ATTRIBUTE.equals(attributeName) || FILE_NAME_GENERATOR_ATTRIBUTE.equals(attributeName))
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);
		BeanDefinition messageMapperDefinition = new RootBeanDefinition(SimpleFileMessageMapper.class);
		messageMapperDefinition.getConstructorArgumentValues().addGenericArgumentValue(
				element.getAttribute(DIRECTORY_ATTRIBUTE));
		if (StringUtils.hasText(element.getAttribute(FILE_NAME_GENERATOR_ATTRIBUTE))) {
			messageMapperDefinition.getPropertyValues().addPropertyValue(NAME_GENERATOR_PROPERTY,
					new RuntimeBeanReference(element.getAttribute(FILE_NAME_GENERATOR_ATTRIBUTE)));
		}
		String mapperBeanName = parserContext.getReaderContext().generateBeanName(messageMapperDefinition);
		parserContext.getRegistry().registerBeanDefinition(
				mapperBeanName, messageMapperDefinition);
		builder.addConstructorArgReference(mapperBeanName);
	}

}
