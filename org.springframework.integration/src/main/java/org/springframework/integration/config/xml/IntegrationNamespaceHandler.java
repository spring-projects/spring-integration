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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.scheduling.SimpleTaskScheduler;

/**
 * Namespace handler for the integration namespace.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class IntegrationNamespaceHandler implements NamespaceHandler {

	private volatile boolean initializedContext;

	private final NamespaceHandlerSupport delegate = new NamespaceHandlerDelegate();


	public void init() {
		this.delegate.init();
	}

	public BeanDefinition parse(Element element, ParserContext parserContext) {
		if (!this.initializedContext) {
			registerTaskSchedulerIfNecessary(parserContext.getRegistry());
			this.initializedContext = true;
		}
		return this.delegate.parse(element, parserContext);
	}	

	public BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext) {
		return this.delegate.decorate(source, definition, parserContext);
	}

	/**
	 * Register a TaskScheduler in the given BeanDefinitionRegistry if not yet present.
	 * The bean name for which this is checking is defined by the constant
	 * {@link IntegrationContextUtils#TASK_SCHEDULER_BEAN_NAME}.
	 */
	private static void registerTaskSchedulerIfNecessary(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME)) {
			RootBeanDefinition errorChannelDef = new RootBeanDefinition(QueueChannel.class);
			BeanDefinitionHolder errorChannelHolder = new BeanDefinitionHolder(
					errorChannelDef, IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(errorChannelHolder, registry);
		}
		TaskExecutor taskExecutor = null;
		if (!registry.containsBeanDefinition(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME)) {
			taskExecutor = IntegrationContextUtils.createThreadPoolTaskExecutor(2, 100, 0, "task-scheduler-");
			BeanDefinitionBuilder schedulerBuilder = BeanDefinitionBuilder.genericBeanDefinition(SimpleTaskScheduler.class);
			schedulerBuilder.addConstructorArgValue(taskExecutor);
			BeanDefinitionBuilder errorHandlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(MessagePublishingErrorHandler.class);
			errorHandlerBuilder.addPropertyReference("defaultErrorChannel", IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
			String errorHandlerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
					errorHandlerBuilder.getBeanDefinition(), registry);
			schedulerBuilder.addPropertyReference("errorHandler", errorHandlerBeanName);
			BeanDefinitionHolder schedulerHolder = new BeanDefinitionHolder(
					schedulerBuilder.getBeanDefinition(), IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME);
			BeanDefinitionReaderUtils.registerBeanDefinition(schedulerHolder, registry);
		}
	}


	private static class NamespaceHandlerDelegate extends NamespaceHandlerSupport {

		public void init() {
			registerBeanDefinitionParser("channel", new PointToPointChannelParser());
			registerBeanDefinitionParser("thread-local-channel", new ThreadLocalChannelParser());
			registerBeanDefinitionParser("publish-subscribe-channel", new PublishSubscribeChannelParser());
			registerBeanDefinitionParser("service-activator", new ServiceActivatorParser());
			registerBeanDefinitionParser("transformer", new TransformerParser());
			registerBeanDefinitionParser("filter", new FilterParser());
			registerBeanDefinitionParser("router", new RouterParser());
			registerBeanDefinitionParser("splitter", new SplitterParser());
			registerBeanDefinitionParser("aggregator", new AggregatorParser());
			registerBeanDefinitionParser("resequencer", new ResequencerParser());
			registerBeanDefinitionParser("inbound-channel-adapter", new MethodInvokingInboundChannelAdapterParser());
			registerBeanDefinitionParser("outbound-channel-adapter", new MethodInvokingOutboundChannelAdapterParser());
			registerBeanDefinitionParser("gateway", new GatewayParser());
			registerBeanDefinitionParser("selector-chain", new SelectorChainParser());
			registerBeanDefinitionParser("annotation-config", new AnnotationConfigParser());
			registerBeanDefinitionParser("application-event-multicaster", new ApplicationEventMulticasterParser());
			registerBeanDefinitionParser("thread-pool-task-executor", new ThreadPoolTaskExecutorParser());
		}
	}

}
