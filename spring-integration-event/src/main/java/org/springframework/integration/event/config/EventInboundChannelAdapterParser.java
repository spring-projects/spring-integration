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
package org.springframework.integration.event.config;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.event.ApplicationEventInboundChannelAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class EventInboundChannelAdapterParser extends AbstractChannelAdapterParser{

	@SuppressWarnings("unchecked")
	@Override
	protected AbstractBeanDefinition doParse(Element element,
			ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder.rootBeanDefinition(ApplicationEventInboundChannelAdapter.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(adapterBuilder, element, "channel", "outputChannel");
		String stringEventTypes = element.getAttribute("event-types");
		if (StringUtils.hasText(stringEventTypes)){
			//Set<String> eventTypes = StringUtils.commaDelimitedListToSet(stringEventTypes);
			StringTokenizer tokenizer = new StringTokenizer(stringEventTypes, " ,");
			Set<Class<? extends ApplicationEvent>> applicationEvents = new HashSet<Class<? extends ApplicationEvent>>();
			while (tokenizer.hasMoreTokens()) {
				String fullyQualifiedClassname = tokenizer.nextToken();
				Class<? extends ApplicationEvent> clazz = 
					(Class<? extends ApplicationEvent>) IntegrationNamespaceUtils.convertFqClassNameToClassObject(fullyQualifiedClassname, parserContext);
				Assert.notNull(clazz, "Class for the event type '" + fullyQualifiedClassname + "' can not be located");
				applicationEvents.add(clazz);
			}
			adapterBuilder.addPropertyValue("eventTypes", applicationEvents);
		}
		//IntegrationNamespaceUtils.setReferenceIfAttributeDefined(adapterBuilder, element, "event", "eventTypes");
		return adapterBuilder.getBeanDefinition();
	}

}
