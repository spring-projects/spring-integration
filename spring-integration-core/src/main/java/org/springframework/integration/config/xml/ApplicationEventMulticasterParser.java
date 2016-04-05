/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.SpringVersion;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;application-event-multicaster&gt; element of the
 * integration namespace.
 *
 * @author Mark Fisher
 */
public class ApplicationEventMulticasterParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.context.event.SimpleApplicationEventMulticaster";
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
			BeanDefinitionBuilder executorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor");
			executorBuilder.addPropertyValue("corePoolSize", 1);
			executorBuilder.addPropertyValue("maxPoolSize", 10);
			executorBuilder.addPropertyValue("queueCapacity", 0);
			executorBuilder.addPropertyValue("threadNamePrefix", "event-multicaster-");
			String executorBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					executorBuilder.getBeanDefinition(), parserContext.getRegistry());
			builder.addPropertyReference("taskExecutor", executorBeanName);
		}
		String springVersion = SpringVersion.getVersion();
		if (springVersion != null && springVersion.startsWith("2")) {
			builder.addPropertyValue("collectionClass", CopyOnWriteArraySet.class);
		}
	}

}
