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

package org.springframework.integration.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.dispatcher.DispatcherPolicy;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>channel</em> element of the integration namespace.
 * 
 * @author Mark Fisher
 */
public class ChannelParser implements BeanDefinitionParser {

	private static final String ID_ATTRIBUTE = "id";

	private static final String CAPACITY_ATTRIBUTE = "capacity";

	private static final String PUBLISH_SUBSCRIBE_ATTRIBUTE = "publish-subscribe";

	private static final String DISPATCHER_POLICY_ELEMENT = "dispatcher-policy";


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		RootBeanDefinition channelDef = new RootBeanDefinition(SimpleChannel.class);
		channelDef.setSource(parserContext.extractSource(element));
		String capacity = element.getAttribute(CAPACITY_ATTRIBUTE);
		if (StringUtils.hasText(capacity)) {
			channelDef.getConstructorArgumentValues().addGenericArgumentValue(Integer.parseInt(capacity));
		}
		boolean isPublishSubscribe = "true".equals(element.getAttribute(PUBLISH_SUBSCRIBE_ATTRIBUTE));
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy(isPublishSubscribe);
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String localName = child.getLocalName();
				if (DISPATCHER_POLICY_ELEMENT.equals(localName)) {
					configureDispatcherPolicy((Element) child, dispatcherPolicy);
				}
			}
		}
		channelDef.getConstructorArgumentValues().addGenericArgumentValue(dispatcherPolicy);
		String beanName = element.getAttribute(ID_ATTRIBUTE);
		parserContext.registerBeanComponent(new BeanComponentDefinition(channelDef, beanName));
		return channelDef;
	}

	private void configureDispatcherPolicy(Element element, DispatcherPolicy dispatcherPolicy) {
		String maxMessagesPerTask = element.getAttribute("max-messages-per-task");
		if (StringUtils.hasText(maxMessagesPerTask)) {
			dispatcherPolicy.setMaxMessagesPerTask(Integer.parseInt(maxMessagesPerTask));
		}
		String receiveTimeout = element.getAttribute("receive-timeout");
		if (StringUtils.hasText(receiveTimeout)) {
			dispatcherPolicy.setReceiveTimeout(Long.parseLong(receiveTimeout));
		}
		String rejectionLimit = element.getAttribute("rejection-limit");
		if (StringUtils.hasText(rejectionLimit)) {
			dispatcherPolicy.setRejectionLimit(Integer.parseInt(rejectionLimit));
		}
		String retryInterval = element.getAttribute("retry-interval");
		if (StringUtils.hasText(retryInterval)) {
			dispatcherPolicy.setRetryInterval(Long.parseLong(retryInterval));
		}
		String shouldFailOnRejectionLimit = element.getAttribute("should-fail-on-rejection-limit");
		if (StringUtils.hasText(shouldFailOnRejectionLimit)) {
			dispatcherPolicy.setShouldFailOnRejectionLimit("true".equals(shouldFailOnRejectionLimit));
		}
	}

}
