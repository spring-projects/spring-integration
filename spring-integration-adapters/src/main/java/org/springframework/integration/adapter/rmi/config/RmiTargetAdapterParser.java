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

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.adapter.rmi.RmiSourceAdapter;
import org.springframework.integration.adapter.rmi.RmiTargetAdapter;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;rmi-target/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class RmiTargetAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return DefaultMessageEndpoint.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		RootBeanDefinition adapterDef = new RootBeanDefinition(RmiTargetAdapter.class);
		String host = element.getAttribute("host");
		String localChannel = element.getAttribute("local-channel");
		String remoteChannel = element.getAttribute("remote-channel");
		if (!(StringUtils.hasText(host) && StringUtils.hasText(localChannel) && StringUtils.hasText(remoteChannel))) {
			throw new MessagingConfigurationException(
					"The 'host', 'local-channel', and 'remote-channel' attributes are all required");
		}
		String url = "rmi://" + host + "/" + RmiSourceAdapter.SERVICE_NAME_PREFIX + remoteChannel;
		adapterDef.getConstructorArgumentValues().addGenericArgumentValue(url);
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
		builder.addPropertyReference("handler", adapterBeanName);
		Subscription subscription = new Subscription(localChannel);
		builder.addPropertyValue("subscription", subscription);
	}

}
