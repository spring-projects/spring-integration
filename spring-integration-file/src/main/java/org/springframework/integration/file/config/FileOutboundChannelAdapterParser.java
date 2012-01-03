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

package org.springframework.integration.file.config;

import org.springframework.integration.file.DefaultFileNameGenerator;
import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element of the 'file'
 * namespace.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class FileOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = FileWritingMessageHandlerBeanDefinitionBuilder.configure(element, "", parserContext);
		if (handlerBuilder != null){
			String remoteFileNameGenerator = element.getAttribute("filename-generator");
			String remoteFileNameGeneratorExpression = element.getAttribute("filename-generator-expression");
			boolean hasRemoteFileNameGenerator = StringUtils.hasText(remoteFileNameGenerator);
			boolean hasRemoteFileNameGeneratorExpression = StringUtils.hasText(remoteFileNameGeneratorExpression);
			if (hasRemoteFileNameGenerator || hasRemoteFileNameGeneratorExpression) {
				if (hasRemoteFileNameGenerator && hasRemoteFileNameGeneratorExpression) {
					parserContext.getReaderContext().error("at most one of 'filename-generator-expression' or 'filename-generator' " +
							"is allowed on file outbound adapter/gateway", element);
				}
				if (hasRemoteFileNameGenerator) {
					handlerBuilder.addPropertyReference("fileNameGenerator", remoteFileNameGenerator);
				}
				else {
					BeanDefinitionBuilder fileNameGeneratorBuilder = BeanDefinitionBuilder
							                                     .genericBeanDefinition(DefaultFileNameGenerator.class);
					fileNameGeneratorBuilder.addPropertyValue("expression", remoteFileNameGeneratorExpression);
					handlerBuilder.addPropertyValue("fileNameGenerator", fileNameGeneratorBuilder.getBeanDefinition());
				}
			}
		}
		
		return (handlerBuilder != null ? handlerBuilder.getBeanDefinition() : null);
	}

}
