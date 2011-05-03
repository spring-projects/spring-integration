/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'outbound-gateway' element of the file namespace.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
public class FileOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		String replyChannel = element.getAttribute("reply-channel");
		
		BeanDefinitionBuilder handlerBuilder = 
			FileWritingMessageHandlerBeanDefinitionBuilder.configure(element, replyChannel, parserContext);
		
		String remoteFileNameGenerator = element.getAttribute("filename-generator");
		String remoteFileNameGeneratorExpression = element.getAttribute("filename-generator-expression");
		boolean hasRemoteFileNameGenerator = StringUtils.hasText(remoteFileNameGenerator);
		boolean hasRemoteFileNameGeneratorExpression = StringUtils.hasText(remoteFileNameGeneratorExpression);
		if (hasRemoteFileNameGenerator || hasRemoteFileNameGeneratorExpression) {
			if (hasRemoteFileNameGenerator && hasRemoteFileNameGeneratorExpression) {
				parserContext.getReaderContext().error("at most one of 'filename-generator-expression' or 'filename-generator' " +
						"is allowed on file outbound adapter/gateway", element) ;
			}
			if (hasRemoteFileNameGenerator) {
				handlerBuilder.addPropertyReference("fileNameGenerator", remoteFileNameGenerator);
			}
			else {
				BeanDefinitionBuilder fileNameGeneratorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						"org.springframework.integration.file.DefaultFileNameGenerator");
				fileNameGeneratorBuilder.addPropertyValue("expression", remoteFileNameGeneratorExpression);
				handlerBuilder.addPropertyValue("fileNameGenerator", fileNameGeneratorBuilder.getBeanDefinition());
			}
		}
		
 		return handlerBuilder;
	}

}
