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
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.util.StringUtils;

/**
 * Base class for <em>target-endpoint</em> and <em>handler-endpoint</em> parsers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractTargetEndpointParser extends AbstractSingleBeanDefinitionParser {

	private static final String INPUT_CHANNEL_ATTRIBUTE = "input-channel";

	private static final String SUBSCRIPTION_PROPERTY = "subscription";

	private static final String SELECTOR_ATTRIBUTE = "selector";

	private static final String SELECTOR_PROPERTY = "messageSelector";

	private static final String PERIOD_ATTRIBUTE = "period";

	private static final String SCHEDULE_ELEMENT = "schedule";

	private static final String INTERCEPTORS_ELEMENT = "interceptors";


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

	protected abstract String getTargetAttributeName();

	protected abstract Class<?> getAdapterClass();

	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		this.parseTarget(element, this.getTargetAttributeName(), parserContext, builder);
		String inputChannel = element.getAttribute(INPUT_CHANNEL_ATTRIBUTE);
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
		if (StringUtils.hasText(inputChannel)) {
			RootBeanDefinition subscriptionDef = new RootBeanDefinition(Subscription.class);
			subscriptionDef.getConstructorArgumentValues().addGenericArgumentValue(inputChannel);
			if (schedule != null) {
				subscriptionDef.getConstructorArgumentValues().addGenericArgumentValue(schedule);
			}
			String subscriptionBeanName = parserContext.getReaderContext().generateBeanName(subscriptionDef);
			parserContext.registerBeanComponent(new BeanComponentDefinition(subscriptionDef, subscriptionBeanName));
			builder.addPropertyReference(SUBSCRIPTION_PROPERTY, subscriptionBeanName);
		}
		String selectorRef = element.getAttribute(SELECTOR_ATTRIBUTE);
		if (StringUtils.hasText(selectorRef)) {
			builder.addPropertyReference(SELECTOR_PROPERTY, selectorRef);
		}
		this.postProcess(builder, element);
	}

	private void parseTarget(Element element, String attribute, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String ref = element.getAttribute(attribute);
		if (!StringUtils.hasText(ref)) {
			throw new ConfigurationException("The '" + attribute + "' attribute is required.");
		}
		String method = element.getAttribute("method");
		if (StringUtils.hasText(method)) {
			String adapterBeanName = this.parseAdapter(ref, method, parserContext);
			builder.addConstructorArgReference(adapterBeanName);
		}
		else {
			builder.addConstructorArgReference(ref);
		}
	}

	private String parseAdapter(String ref, String method, ParserContext parserContext) {
		BeanDefinition adapterDef = new RootBeanDefinition(this.getAdapterClass());
		adapterDef.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(ref));
		adapterDef.getPropertyValues().addPropertyValue("methodName", method);
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
		return adapterBeanName;
	}

	private Schedule parseSchedule(Element scheduleElement) {
		PollingSchedule schedule = new PollingSchedule(5);
		String period = scheduleElement.getAttribute(PERIOD_ATTRIBUTE);
		if (StringUtils.hasText(period)) {
			schedule.setPeriod(Integer.parseInt(period));
		}
		return schedule;
	}

}
