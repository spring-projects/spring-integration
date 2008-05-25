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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.file.ByteArrayFileMessageCreator;
import org.springframework.integration.adapter.file.FileMessageCreator;
import org.springframework.integration.adapter.file.TextFileMessageCreator;
import org.springframework.util.StringUtils;

/**
 * Base class for directory-based sources.
 * 
 * @author Marius Bogoevici
 */
public abstract class AbstractDirectorySourceParser extends AbstractSimpleBeanDefinitionParser {

	public static final String MESSAGE_CREATOR_REFERENCE_ATTRIBUTE = "message-creator";

	public static final String TYPE_ATTRIBUTE = "type";


	private final boolean deleteFileAfterMessageCreation;


	public AbstractDirectorySourceParser(boolean deleteFileAfterMessageCreation){
		this.deleteFileAfterMessageCreation = deleteFileAfterMessageCreation;
	}


	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !MESSAGE_CREATOR_REFERENCE_ATTRIBUTE.equals(attributeName) 
				&& !TYPE_ATTRIBUTE.equals(attributeName)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
		String messageCreatorReference = element.getAttribute(MESSAGE_CREATOR_REFERENCE_ATTRIBUTE);
		String type = element.getAttribute(TYPE_ATTRIBUTE);
		if (StringUtils.hasText(type) && StringUtils.hasText(messageCreatorReference)) {
			throw new ConfigurationException(
					"Either the 'type' or the 'message-creator' attributes are allowed, but not both.");
		}
		if (StringUtils.hasText(messageCreatorReference)) {
			beanDefinition.addConstructorArgReference(messageCreatorReference);
		}
		else {
			if ("text".equals(type)) {
				beanDefinition.addConstructorArgValue(new TextFileMessageCreator(deleteFileAfterMessageCreation));
			}
			else if ("binary".equals(type)) {
				beanDefinition.addConstructorArgValue(new ByteArrayFileMessageCreator(deleteFileAfterMessageCreation));
			}
			else {
				beanDefinition.addConstructorArgValue(new FileMessageCreator());
			}
		}
	}

}
