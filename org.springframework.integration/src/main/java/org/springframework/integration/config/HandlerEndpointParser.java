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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.handler.DefaultMessageHandlerAdapter;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>handler-endpoint</em> element of the integration namespace.
 * 
 * @author Mark Fisher
 */
public class HandlerEndpointParser extends AbstractSingleBeanDefinitionParser {

	private static final String INPUT_CHANNEL_ATTRIBUTE = "input-channel";

	private static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";

	private static final String OUTPUT_CHANNEL_PROPERTY = "outputChannelName";

	private static final String RETURN_ADDRESS_OVERRIDES_ATTRIBUTE = "return-address-overrides";

	private static final String REPLY_HANDLER_ATTRIBUTE = "reply-handler";

	private static final String REPLY_HANDLER_PROPERTY = "replyHandler";

	private static final String SELECTOR_ATTRIBUTE = "selector";

	private static final String SELECTOR_PROPERTY = "messageSelector";

	private static final String PERIOD_ATTRIBUTE = "period";

	private static final String SCHEDULE_ELEMENT = "schedule";

	private static final String INTERCEPTORS_ELEMENT = "interceptors";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return HandlerEndpoint.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String handler = element.getAttribute("handler");
		if (!StringUtils.hasText(handler)) {
			throw new ConfigurationException("The 'handler' attribute is required.");
		}
		String method = element.getAttribute("method");
		if (StringUtils.hasText(method)) {
			String adapterBeanName = this.parseAdapter(handler, method, parserContext);
			builder.addConstructorArgReference(adapterBeanName);
		}
		else {
			builder.addConstructorArgReference(handler);
		}
		String inputChannelName = element.getAttribute(INPUT_CHANNEL_ATTRIBUTE);
		Schedule schedule = null;
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				String localName = child.getLocalName();
				if (SCHEDULE_ELEMENT.equals(localName)) {
					schedule = this.parseSchedule(childElement);
				}
				else if (INTERCEPTORS_ELEMENT.equals(localName)) {
					EndpointInterceptorParser parser = new EndpointInterceptorParser();
					ManagedList interceptors = parser.parseEndpointInterceptors(childElement, parserContext);
					builder.addPropertyValue("interceptors", interceptors);
				}
			}
		}
		if (StringUtils.hasText(inputChannelName)) {
			builder.addPropertyValue("inputChannelName", inputChannelName);
		}
		if (schedule != null) {
			builder.addPropertyValue("schedule", schedule);
		}
		String selectorRef = element.getAttribute(SELECTOR_ATTRIBUTE);
		if (StringUtils.hasText(selectorRef)) {
			builder.addPropertyReference(SELECTOR_PROPERTY, selectorRef);
		}
		String outputChannel = element.getAttribute(OUTPUT_CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(outputChannel)) {
			builder.addPropertyValue(OUTPUT_CHANNEL_PROPERTY, outputChannel);
		}
		String returnAddressOverridesAttribute = element.getAttribute(RETURN_ADDRESS_OVERRIDES_ATTRIBUTE);
		boolean returnAddressOverrides = "true".equals(returnAddressOverridesAttribute);
		builder.addPropertyValue("returnAddressOverrides", returnAddressOverrides);
		String replyHandler = element.getAttribute(REPLY_HANDLER_ATTRIBUTE);
		if (StringUtils.hasText(replyHandler)) {
			builder.addPropertyValue(REPLY_HANDLER_PROPERTY, new RuntimeBeanReference(replyHandler));
		}
	}

	private String parseAdapter(String ref, String method, ParserContext parserContext) {
		BeanDefinition adapterDef = new RootBeanDefinition(DefaultMessageHandlerAdapter.class);
		adapterDef.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(ref));
		adapterDef.getPropertyValues().addPropertyValue("methodName", method);
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
		return adapterBeanName;
	}

	private Schedule parseSchedule(Element scheduleElement) {
		PollingSchedule schedule = new PollingSchedule(0);
		String period = scheduleElement.getAttribute(PERIOD_ATTRIBUTE);
		if (StringUtils.hasText(period)) {
			schedule.setPeriod(Integer.parseInt(period));
		}
		return schedule;
	}

}
