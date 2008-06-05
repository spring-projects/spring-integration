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

package org.springframework.integration.channel.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.interceptor.MessageSelectingInterceptor;
import org.springframework.integration.message.selector.PayloadTypeSelector;
import org.springframework.util.StringUtils;

/**
 * Base class for channel parsers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractChannelParser extends AbstractSingleBeanDefinitionParser {

	private static final String PUBLISH_SUBSCRIBE_ATTRIBUTE = "publish-subscribe";

	private static final String DISPATCHER_POLICY_ELEMENT = "dispatcher-policy";

	private static final String DATATYPE_ATTRIBUTE = "datatype";

	private static final String INTERCEPTOR_ELEMENT = "interceptor";

	private static final String INTERCEPTORS_PROPERTY = "interceptors";

	private static final String SECURED_ELEMENT ="secured";


	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected abstract Class<?> getBeanClass(Element element);

	protected abstract void configureConstructorArgs(
			BeanDefinitionBuilder builder, Element element, DispatcherPolicy dispatcherPolicy);

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		boolean isPublishSubscribe = "true".equals(element.getAttribute(PUBLISH_SUBSCRIBE_ATTRIBUTE));
		DispatcherPolicy dispatcherPolicy = new DispatcherPolicy(isPublishSubscribe);
		ManagedList interceptors = new ManagedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String localName = child.getLocalName();
				if (DISPATCHER_POLICY_ELEMENT.equals(localName)) {
					configureDispatcherPolicy((Element) child, dispatcherPolicy);
				}
				else if (INTERCEPTOR_ELEMENT.equals(localName)) {
					String ref = ((Element) child).getAttribute("ref");
					interceptors.add(new RuntimeBeanReference(ref));
				}
				else if (SECURED_ELEMENT.equals(localName)) {
					parserContext.getReaderContext().getNamespaceHandlerResolver().resolve(
							element.getNamespaceURI()).parse((Element)child, parserContext);
				}
			}
		}
		String datatypeAttr = element.getAttribute(DATATYPE_ATTRIBUTE);
		if (StringUtils.hasText(datatypeAttr)) {
			String[] datatypes = StringUtils.commaDelimitedListToStringArray(datatypeAttr);
			RootBeanDefinition selectorDef = new RootBeanDefinition(PayloadTypeSelector.class);
			selectorDef.getConstructorArgumentValues().addGenericArgumentValue(datatypes);
			String selectorBeanName = parserContext.getReaderContext().generateBeanName(selectorDef);
			BeanComponentDefinition selectorComponent = new BeanComponentDefinition(selectorDef, selectorBeanName);
			parserContext.registerBeanComponent(selectorComponent);
			RootBeanDefinition interceptorDef = new RootBeanDefinition(MessageSelectingInterceptor.class);
			interceptorDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(selectorBeanName));
			String interceptorBeanName = parserContext.getReaderContext().generateBeanName(interceptorDef);
			BeanComponentDefinition interceptorComponent = new BeanComponentDefinition(interceptorDef, interceptorBeanName);
			parserContext.registerBeanComponent(interceptorComponent);
			interceptors.add(new RuntimeBeanReference(interceptorBeanName));
		}
		builder.addPropertyValue(INTERCEPTORS_PROPERTY, interceptors);
		this.configureConstructorArgs(builder, element, dispatcherPolicy);
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
