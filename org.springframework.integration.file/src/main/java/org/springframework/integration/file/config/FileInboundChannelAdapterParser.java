/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
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
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_NAME + ".FileReadingMessageSource");
		String comparator = element.getAttribute("comparator");
		if (StringUtils.hasText(comparator)) {
			builder.addConstructorArgReference(comparator);
		}
		String directory = element.getAttribute("directory");
		if (StringUtils.hasText(directory)) {
			if (directory.indexOf(':') == -1) {
				directory = "file:" + directory;
			}
			builder.addPropertyValue("inputDirectory", directory);
		}
		String filterBeanName = this.registerFileListFilter(element, parserContext);
		builder.addPropertyReference("filter", filterBeanName);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private String registerFileListFilter(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder factoryBeanBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_NAME + ".config.FileListFilterFactoryBean");
		factoryBeanBuilder.setRole(BeanDefinition.ROLE_SUPPORT);
		String filter = element.getAttribute("filter");
		if (StringUtils.hasText(filter)) {
			factoryBeanBuilder.addPropertyReference("filterReference", filter);
		}
		String filenamePattern = element.getAttribute("filename-pattern");
		if (StringUtils.hasText(filenamePattern)) {
			if (StringUtils.hasText(filter)) {
				parserContext.getReaderContext().error(
						"At most one of 'filter' and 'filename-pattern' may be provided.", element);
			}
			factoryBeanBuilder.addPropertyValue("filenamePattern", filenamePattern);
		}
		String preventDuplicates = element.getAttribute("prevent-duplicates");
		if (StringUtils.hasText(preventDuplicates)) {
			factoryBeanBuilder.addPropertyValue("preventDuplicates", preventDuplicates);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				factoryBeanBuilder.getBeanDefinition(), parserContext.getRegistry());
	}

}
