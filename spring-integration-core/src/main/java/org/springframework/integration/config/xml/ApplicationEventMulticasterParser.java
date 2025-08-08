/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;application-event-multicaster&gt; element of the
 * integration namespace.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class ApplicationEventMulticasterParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return SimpleApplicationEventMulticaster.class;
	}

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		return AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String taskExecutorRef = element.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorRef)) {
			builder.addPropertyReference("taskExecutor", taskExecutorRef);
		}
		else {
			BeanDefinitionBuilder executorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ThreadPoolTaskExecutor.class);
			executorBuilder.addPropertyValue("corePoolSize", 1); // NOSONAR
			executorBuilder.addPropertyValue("maxPoolSize", 10); // NOSONAR
			executorBuilder.addPropertyValue("queueCapacity", 0); // NOSONAR
			executorBuilder.addPropertyValue("threadNamePrefix", "event-multicaster-");
			builder.addPropertyValue("taskExecutor", executorBuilder.getBeanDefinition());
		}
	}

}
