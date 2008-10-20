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
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.bus.ApplicationContextMessageBus;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.bus.MessageBusAwareBeanPostProcessor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.annotation.MessagingAnnotationPostProcessor;
import org.springframework.integration.config.annotation.PublisherAnnotationPostProcessor;
import org.springframework.integration.scheduling.SimpleTaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;message-bus&gt; element of the integration namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessageBusParser extends AbstractSimpleBeanDefinitionParser {

	public static final String MESSAGE_BUS_BEAN_NAME = "internal.MessageBus";

	public static final String MESSAGE_BUS_AWARE_POST_PROCESSOR_BEAN_NAME =
			"internal.MessageBusAwareBeanPostProcessor";

	private static final String PUBLISHER_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.PublisherAnnotationPostProcessor";

	private static final String MESSAGING_ANNOTATION_POST_PROCESSOR_BEAN_NAME =
			"internal.MessagingAnnotationPostProcessor";

	private static final String TASK_SCHEDULER_ATTRIBUTE = "task-scheduler";

	private static final String ASYNC_EVENT_MULTICASTER_ATTRIBUTE = "configure-async-event-multicaster";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		Assert.state(!parserContext.getRegistry().containsBeanDefinition(MESSAGE_BUS_BEAN_NAME),
				"Only one instance of '" + MessageBus.class.getSimpleName() + "' is allowed per ApplicationContext.");
		return MESSAGE_BUS_BEAN_NAME;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return ApplicationContextMessageBus.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !TASK_SCHEDULER_ATTRIBUTE.equals(attributeName) &&
				!ASYNC_EVENT_MULTICASTER_ATTRIBUTE.equals(attributeName) &&
				!"enable-annotations".equals(attributeName) &&
				super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);
		String taskSchedulerRef = element.getAttribute(TASK_SCHEDULER_ATTRIBUTE);
		TaskExecutor taskExecutor= null;
		if (!parserContext.getRegistry().containsBeanDefinition(ApplicationContextMessageBus.ERROR_CHANNEL_BEAN_NAME)) {
			RootBeanDefinition errorChannelDef = new RootBeanDefinition(QueueChannel.class);
			BeanDefinitionHolder errorChannelHolder = new BeanDefinitionHolder(
					errorChannelDef, ApplicationContextMessageBus.ERROR_CHANNEL_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(errorChannelHolder, parserContext.getRegistry());
		}
		if (StringUtils.hasText(taskSchedulerRef)) {
			builder.addPropertyReference("taskScheduler", taskSchedulerRef);
		}
		else {
			taskExecutor = this.createTaskExecutor(100, "message-bus-");
			BeanDefinitionBuilder schedulerBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleTaskScheduler.class);
			schedulerBuilder.addConstructorArgValue(taskExecutor);
			// TODO: define bean name as a constant elsewhere
			String TASK_SCHEDULER_BEAN_NAME = "taskScheduler";
			BeanDefinitionHolder schedulerHolder = new BeanDefinitionHolder(
					schedulerBuilder.getBeanDefinition(), TASK_SCHEDULER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(schedulerHolder, parserContext.getRegistry());
			builder.addPropertyReference("taskScheduler", TASK_SCHEDULER_BEAN_NAME);
		}
		if ("true".equals(element.getAttribute(ASYNC_EVENT_MULTICASTER_ATTRIBUTE).toLowerCase())) {
			if (taskExecutor == null) {
				taskExecutor = this.createTaskExecutor(10, "event-multicaster-");
			}
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

	private TaskExecutor createTaskExecutor(int corePoolSize, String threadPrefix) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(corePoolSize);
		if (StringUtils.hasText(threadPrefix)) {
			executor.setThreadFactory(new CustomizableThreadFactory(threadPrefix));
		}
		executor.setRejectedExecutionHandler(new CallerRunsPolicy());
		executor.afterPropertiesSet();
		return executor;
	}

	/**
	 * Adds extra post-processors to the context, to inject the objects configured by the MessageBus
	 */
	private void addPostProcessors(Element element, ParserContext parserContext) {
		this.registerMessageBusAwarePostProcessor(parserContext);
		if ("true".equals(element.getAttribute("enable-annotations").toLowerCase())) {
			this.registerPublisherPostProcessor(parserContext);
			this.registerMessagingAnnotationPostProcessor(parserContext);
		}
	}

	private void registerMessageBusAwarePostProcessor(ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessageBusAwareBeanPostProcessor.class);
		builder.addConstructorArgReference(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
		builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserContext.getRegistry().registerBeanDefinition(MESSAGE_BUS_AWARE_POST_PROCESSOR_BEAN_NAME, builder.getBeanDefinition());
	}

	private void registerPublisherPostProcessor(ParserContext parserContext) {
		BeanDefinition bd = new RootBeanDefinition(PublisherAnnotationPostProcessor.class);
		BeanComponentDefinition bcd = new BeanComponentDefinition(bd, PUBLISHER_ANNOTATION_POST_PROCESSOR_BEAN_NAME);
		parserContext.registerBeanComponent(bcd);
	}

	private void registerMessagingAnnotationPostProcessor(ParserContext parserContext) {
		BeanDefinition bd = new RootBeanDefinition(MessagingAnnotationPostProcessor.class);
		BeanComponentDefinition bcd = new BeanComponentDefinition(
				bd, MESSAGING_ANNOTATION_POST_PROCESSOR_BEAN_NAME);
		parserContext.registerBeanComponent(bcd);
	}

}
