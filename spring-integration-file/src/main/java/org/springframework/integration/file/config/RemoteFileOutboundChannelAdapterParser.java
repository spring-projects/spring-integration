/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;

import reactor.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @since 2.0
 */
public abstract class RemoteFileOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(FileTransferringMessageHandler.class);

		BeanDefinition templateDefinition = FileParserUtils.parseRemoteFileTemplate(element, parserContext, true,
				getTemplateClass());

		handlerBuilder.addConstructorArgValue(templateDefinition);
		String mode = element.getAttribute("mode");
		if (StringUtils.hasText(mode)) {
			handlerBuilder.addConstructorArgValue(mode);
		}
		return handlerBuilder.getBeanDefinition();
	}

	protected abstract Class<? extends RemoteFileOperations<?>> getTemplateClass();

}
