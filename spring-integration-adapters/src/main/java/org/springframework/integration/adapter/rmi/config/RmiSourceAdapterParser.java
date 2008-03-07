/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.adapter.rmi.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.adapter.rmi.RmiSourceAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;rmi-source/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class RmiSourceAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return RmiSourceAdapter.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String channelRef = element.getAttribute("channel");
		if (!StringUtils.hasText(channelRef)) {
			throw new MessagingConfigurationException("a 'channel' reference is required");
		}
		builder.addPropertyReference("channel", channelRef);
		builder.addPropertyValue("expectReply", element.getAttribute("expect-reply").equals("true"));
		String sendTimeout = element.getAttribute("send-timeout");
		if (StringUtils.hasText(sendTimeout)) {
			builder.addPropertyValue("sendTimeout", Long.parseLong(sendTimeout));
		}
		String receiveTimeout = element.getAttribute("receive-timeout");
		if (StringUtils.hasText(receiveTimeout)) {
			builder.addPropertyValue("receiveTimeout", Long.parseLong(receiveTimeout));
		}
	}

}
