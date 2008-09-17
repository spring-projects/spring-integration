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

package org.springframework.integration.stream.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.stream.CharacterStreamTarget;
import org.springframework.util.StringUtils;

/**
 * Parser for the "stdout-" and "stderr-channel-adapter" elements.
 * 
 * @author Mark Fisher
 */
public class ConsoleTargetParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return CharacterStreamTarget.class;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) throws BeanDefinitionStoreException {
		String id = element.getAttribute("id");
		if (!element.hasAttribute("channel")) {
			// the created channel will get the 'id', so the adapter's bean name includes a suffix
			id = id + ".adapter";
		}
		else if (!StringUtils.hasText(id)) {
			id = parserContext.getReaderContext().generateBeanName(definition);
		}
		return id;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		if (element.getLocalName().startsWith("stderr")) {
			builder.setFactoryMethod("stderr");
		}
		else {
			builder.setFactoryMethod("stdout");
		}
		String charsetName = element.getAttribute("charset");
		if (StringUtils.hasText(charsetName)) {
			builder.addConstructorArgValue(charsetName);
		}
		if ("true".equals(element.getAttribute("append-newline"))) {
			builder.addPropertyValue("shouldAppendNewLine", Boolean.TRUE);
		}
		String channelName = element.getAttribute("channel");
		if (StringUtils.hasText(channelName)) {
			builder.addPropertyReference("inputChannel", channelName);
		}
		else {
			builder.addPropertyReference("inputChannel", this.createDirectChannel(element, parserContext));
		}
	}

	private String createDirectChannel(Element element, ParserContext parserContext) {
		String channelId = element.getAttribute("id");
		if (!StringUtils.hasText(channelId)) {
			throw new ConfigurationException("The channel-adapter's 'id' attribute is required when no 'channel' "
					+ "reference has been provided, because that 'id' would be used for the created channel.");
		}
		BeanDefinitionBuilder channelBuilder = BeanDefinitionBuilder.genericBeanDefinition(DirectChannel.class);
		BeanDefinitionHolder holder = new BeanDefinitionHolder(channelBuilder.getBeanDefinition(), channelId);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		return channelId;
	}

}
