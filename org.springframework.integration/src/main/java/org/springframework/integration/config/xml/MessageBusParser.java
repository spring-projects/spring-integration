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

package org.springframework.integration.config.xml;

import java.util.concurrent.CopyOnWriteArraySet;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.bus.ApplicationContextMessageBus;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * Parser for the &lt;message-bus&gt; element of the integration namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessageBusParser extends AbstractSimpleBeanDefinitionParser {

	private static final String MESSAGING_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.MessagingAnnotationPostProcessor";

	private static final String ASYNC_EVENT_MULTICASTER_ATTRIBUTE = "configure-async-event-multicaster";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return ApplicationContextMessageBus.class;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		return "messageBus";
	}


	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !ASYNC_EVENT_MULTICASTER_ATTRIBUTE.equals(attributeName) &&
				!"enable-annotations".equals(attributeName) &&
				super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);
		IntegrationNamespaceUtils.registerTaskSchedulerIfNecessary(parserContext.getRegistry());
		if ("true".equals(element.getAttribute(ASYNC_EVENT_MULTICASTER_ATTRIBUTE).toLowerCase())) {
			TaskExecutor taskExecutor = IntegrationContextUtils.createTaskExecutor(1, 10, 0, "event-multicaster-");
			BeanDefinitionBuilder eventMulticasterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					SimpleApplicationEventMulticaster.class);
			eventMulticasterBuilder.addPropertyValue("taskExecutor", taskExecutor);
			eventMulticasterBuilder.addPropertyValue("collectionClass", CopyOnWriteArraySet.class);
			BeanDefinitionHolder holder = new BeanDefinitionHolder(eventMulticasterBuilder.getBeanDefinition(),
					AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		}
		this.addPostProcessors(element, parserContext);
	}

	/**
	 * Adds extra post-processors to the context, to inject the objects configured by the MessageBus
	 */
	private void addPostProcessors(Element element, ParserContext parserContext) {
		if ("true".equals(element.getAttribute("enable-annotations").toLowerCase())) {
			this.registerMessagingAnnotationPostProcessor(parserContext);
		}
	}

	private void registerMessagingAnnotationPostProcessor(ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessagingAnnotationPostProcessor.class);
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserContext.getRegistry().registerBeanDefinition(
				MESSAGING_ANNOTATION_POST_PROCESSOR_BEAN_NAME, builder.getBeanDefinition());
	}

}
