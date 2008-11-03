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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element of the 'file' namespace.
 * 
 * @author Mark Fisher
 */
public class FileOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		String directory = element.getAttribute("directory");
		Assert.hasText(directory, "directory is required");
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FileWritingMessageHandler.class);
		builder.addConstructorArgValue(directory);
		String fileNameGenerator = element.getAttribute("filename-generator");
		if (StringUtils.hasText(fileNameGenerator)) {
			builder.addPropertyReference("fileNameGenerator", fileNameGenerator);
		}
		return builder.getBeanDefinition();
	}

}
