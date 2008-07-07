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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.router.SplitterMessageHandlerAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;splitter/&gt; element.
 * 
 * @author Mark Fisher
 */
public class SplitterParser extends AbstractHandlerEndpointParser {

	@Override
	protected Class<? extends MessageHandler> getHandlerAdapterClass() {
		return SplitterMessageHandlerAdapter.class;
	}

	@Override
	protected void postProcessAdapterBean(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String outputChannelName = element.getAttribute("output-channel");
		if (!StringUtils.hasText(outputChannelName)) {
			throw new ConfigurationException("The 'output-channel' attribute is required.");
		}
		builder.addPropertyValue("outputChannelName", outputChannelName);
	}

}
