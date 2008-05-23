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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.file.ByteArrayFileMessageCreator;
import org.springframework.integration.adapter.file.FileMessageCreator;
import org.springframework.integration.adapter.file.FileSource;
import org.springframework.integration.adapter.file.TextFileMessageCreator;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;file-source/&gt; element.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FileSourceParser extends AbstractSimpleBeanDefinitionParser {

	private static final String FILE_SOURCE_TYPE_ATTRIBUTE = "file";
	
	private static final String TEXT_SOURCE_TYPE_ATTRIBUTE = "text";
	
	private static final String BINARY_SOURCE_TYPE_ATTRIBUTE = "binary";

	public static final String DIRECTORY_ATTRIBUTE = "directory";

	public static final String MESSAGE_CREATOR_REFERENCE_ATTRIBUTE = "message-creator";

	public static final String TYPE_ATTRIBUTE = "type";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return FileSource.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !(DIRECTORY_ATTRIBUTE.equals(attributeName) || MESSAGE_CREATOR_REFERENCE_ATTRIBUTE.equals(attributeName) || TYPE_ATTRIBUTE
				.equals(attributeName))
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
		beanDefinition.addConstructorArgValue(element.getAttribute(DIRECTORY_ATTRIBUTE));
		String messageCreatorReference = element.getAttribute(MESSAGE_CREATOR_REFERENCE_ATTRIBUTE);
		String type = element.getAttribute(TYPE_ATTRIBUTE);
		if (StringUtils.hasText(type) && StringUtils.hasText(messageCreatorReference)) {
			throw new ConfigurationException(
					"Either the 'type' or the 'message-creator' attributes are allowed, but not both");
		}
		if (StringUtils.hasText(messageCreatorReference)) {
			beanDefinition.addConstructorArgReference(messageCreatorReference);
		}
		else {
			if (!StringUtils.hasText(type) || FILE_SOURCE_TYPE_ATTRIBUTE.equals(type)) {
				beanDefinition.addConstructorArgValue(new FileMessageCreator());
			}
			else if (TEXT_SOURCE_TYPE_ATTRIBUTE.equals(type)) {
				beanDefinition.addConstructorArgValue(new TextFileMessageCreator());
			}
			else if (BINARY_SOURCE_TYPE_ATTRIBUTE.equals(type)) {
				beanDefinition.addConstructorArgValue(new ByteArrayFileMessageCreator());
			}
		}
	}

}
