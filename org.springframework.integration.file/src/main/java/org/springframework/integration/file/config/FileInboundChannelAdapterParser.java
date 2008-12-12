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

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of the 'file' namespace.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class FileInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	private static final String PACKAGE_NAME = "org.springframework.integration.file";


	@Override
	@SuppressWarnings("unchecked")
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_NAME + ".FileReadingMessageSource");
		String directory = element.getAttribute("directory");
		if (StringUtils.hasText(directory)) {
			builder.addPropertyValue("inputDirectory", directory);
		}
		String filter = element.getAttribute("filter");
		if (StringUtils.hasText(filter)){
			builder.addPropertyReference("filter", filter);
		}
		String filenamePattern = element.getAttribute("filename-pattern");
		if (StringUtils.hasText(filenamePattern)) {
			if (StringUtils.hasText(filter)) {
				parserContext.getReaderContext().error(
						"At most one of 'filter' and 'filename-pattern' may be provided.", element);
			}
			String acceptOnceFilterBeanName = this.parseFilter("AcceptOnceFileListFilter", null, parserContext);
			String patternFilterBeanName = this.parseFilter("PatternMatchingFileListFilter", filenamePattern, parserContext);
			ManagedList filters = new ManagedList();
			filters.add(new RuntimeBeanReference(acceptOnceFilterBeanName));
			filters.add(new RuntimeBeanReference(patternFilterBeanName));
			String compositeFilterBeanName = this.parseFilter("CompositeFileListFilter", filters, parserContext);
			builder.addPropertyReference("filter", compositeFilterBeanName);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private String parseFilter(String shortClassName, Object constructorArgValue, ParserContext parserContext) {
		BeanDefinitionBuilder filterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_NAME + "." + shortClassName);
		filterBuilder.getBeanDefinition().setRole(BeanDefinition.ROLE_SUPPORT);
		if (constructorArgValue != null) {
			filterBuilder.addConstructorArgValue(constructorArgValue);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				filterBuilder.getBeanDefinition(), parserContext.getRegistry());
	}

}
