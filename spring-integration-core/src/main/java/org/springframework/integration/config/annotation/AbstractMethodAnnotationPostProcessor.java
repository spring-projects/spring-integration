/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for Method-level annotation post-processors.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class AbstractMethodAnnotationPostProcessor<T extends Annotation> implements MethodAnnotationPostProcessor<T> {

	private static final String INPUT_CHANNEL_ATTRIBUTE = "inputChannel";

	private static final String ADVICE_CHAIN_ATTRIBUTE = "adviceChain";


	protected final BeanFactory beanFactory;

	protected final Environment environment;

	protected final DestinationResolver<MessageChannel> channelResolver;

	protected final Class<T> annotationType;

	@SuppressWarnings("unchecked")
	public AbstractMethodAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.environment = environment;
		this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		this.annotationType = (Class<T>) GenericTypeResolver.resolveTypeArgument(this.getClass(),
				MethodAnnotationPostProcessor.class);
	}


	@Override
	public Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations) {
		MessageHandler handler = this.createHandler(bean, method, annotations);
		this.setAdviceChainIfPresent(beanName, annotations, handler);
		if (handler instanceof Orderable) {
			Order orderAnnotation = AnnotationUtils.findAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				((Orderable) handler).setOrder(orderAnnotation.value());
			}
		}
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			String handlerBeanName = this.generateHandlerBeanName(beanName, method);
			ConfigurableListableBeanFactory listableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
			listableBeanFactory.registerSingleton(handlerBeanName, handler);
			handler = (MessageHandler) listableBeanFactory.initializeBean(handler, handlerBeanName);
		}
		AbstractEndpoint endpoint = this.createEndpoint(handler, annotations);
		if (endpoint != null) {
			return endpoint;
		}
		return handler;
	}

	@Override
	public boolean shouldCreateEndpoint(Method method, List<Annotation> annotations) {
		String inputChannel = MessagingAnnotationUtils.resolveAttribute(annotations, "inputChannel", String.class);
		return StringUtils.hasText(inputChannel);
	}


	protected final void setAdviceChainIfPresent(String beanName, List<Annotation> annotations, MessageHandler handler) {
		String[] adviceChainNames = MessagingAnnotationUtils.resolveAttribute(annotations, ADVICE_CHAIN_ATTRIBUTE,
				String[].class);
		/*
		 * Note: we don't merge advice chain contents; if the directAnnotation has a non-empty
		 * attribute, it wins. You cannot "remove" an advice chain from a meta-annotation
		 * by setting an empty array on the custom annotation.
		 */
		if (adviceChainNames != null && adviceChainNames.length > 0) {
			if (!(handler instanceof AbstractReplyProducingMessageHandler)) {
				throw new IllegalArgumentException("Cannot apply advice chain to " + handler.getClass().getName());
			}
			List<Advice> adviceChain = new ArrayList<Advice>();
			for (String adviceChainName : adviceChainNames) {
				Object adviceChainBean = this.beanFactory.getBean(adviceChainName);
				if (adviceChainBean instanceof Advice) {
					adviceChain.add((Advice) adviceChainBean);
				}
				else if (adviceChainBean instanceof Advice[]) {
					Collections.addAll(adviceChain, (Advice[]) adviceChainBean);
				}
				else if (adviceChainBean instanceof Collection) {
					@SuppressWarnings("unchecked")
					Collection<Advice> adviceChainEntries = (Collection<Advice>) adviceChainBean;
					for (Advice advice : adviceChainEntries) {
						adviceChain.add(advice);
					}
				}
				else {
					throw new IllegalArgumentException("Invalid advice chain type:" +
						adviceChainName.getClass().getName() + " for bean '" + beanName + "'");
				}
			}
			((AbstractReplyProducingMessageHandler) handler).setAdviceChain(adviceChain);
		}
	}

	protected String getInputChannelAttribute() {
		return INPUT_CHANNEL_ATTRIBUTE;
	}

	private AbstractEndpoint createEndpoint(MessageHandler handler, List<Annotation> annotations) {
		AbstractEndpoint endpoint = null;
		String inputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, getInputChannelAttribute(),
				String.class);
		if (StringUtils.hasText(inputChannelName)) {
			MessageChannel inputChannel;
			try {
				inputChannel = this.channelResolver.resolveDestination(inputChannelName);
			}
			catch (DestinationResolutionException e) {
				inputChannel = new DirectChannel();
				if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
					ConfigurableListableBeanFactory listableBeanFactory = (ConfigurableListableBeanFactory) this.beanFactory;
					listableBeanFactory.registerSingleton(inputChannelName, inputChannel);
					inputChannel = (MessageChannel) listableBeanFactory.initializeBean(inputChannel, inputChannelName);
				}
			}
			Assert.notNull(inputChannel, "failed to resolve inputChannel '" + inputChannelName + "'");

			if (inputChannel instanceof PollableChannel) {
				PollingConsumer pollingConsumer = new PollingConsumer((PollableChannel) inputChannel, handler);
				this.configurePollingEndpoint(pollingConsumer, annotations);
				endpoint = pollingConsumer;
			}
			else {
				Poller[] pollers = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller[].class);
				Assert.state(ObjectUtils.isEmpty(pollers), "A '@Poller' should not be specified for Annotation-based " +
						"endpoint, since '" + inputChannel + "' is a SubscribableChannel (not pollable).");
				endpoint = new EventDrivenConsumer((SubscribableChannel) inputChannel, handler);
			}
		}
		return endpoint;
	}

	protected void configurePollingEndpoint(AbstractPollingEndpoint pollingEndpoint, List<Annotation> annotations) {
		PollerMetadata pollerMetadata = null;
		Poller[] pollers = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller[].class);
		if (!ObjectUtils.isEmpty(pollers)) {
			Assert.state(pollers.length == 1, "The 'poller' for an Annotation-based endpoint can have only one '@Poller'.");
			Poller poller = pollers[0];

			String ref = poller.value();
			String triggerRef = poller.trigger();
			String executorRef = poller.taskExecutor();
			String fixedDelayValue = this.environment.resolvePlaceholders(poller.fixedDelay());
			String fixedRateValue = this.environment.resolvePlaceholders(poller.fixedRate());
			String maxMessagesPerPollValue = this.environment.resolvePlaceholders(poller.maxMessagesPerPoll());
			String cron = this.environment.resolvePlaceholders(poller.cron());

			if (StringUtils.hasText(ref)) {
				Assert.state(!StringUtils.hasText(triggerRef) && !StringUtils.hasText(executorRef) &&
								!StringUtils.hasText(cron) && !StringUtils.hasText(fixedDelayValue) &&
								!StringUtils.hasText(fixedRateValue) && !StringUtils.hasText(maxMessagesPerPollValue),
						"The '@Poller' 'ref' attribute is mutually exclusive with other attributes.");
				pollerMetadata = this.beanFactory.getBean(ref, PollerMetadata.class);
			}
			else {
				pollerMetadata = new PollerMetadata();
				if (StringUtils.hasText(maxMessagesPerPollValue)) {
					pollerMetadata.setMaxMessagesPerPoll(Long.parseLong(maxMessagesPerPollValue));
				}
				if (StringUtils.hasText(executorRef)) {
					pollerMetadata.setTaskExecutor(this.beanFactory.getBean(executorRef, TaskExecutor.class));
				}
				Trigger trigger = null;
				if (StringUtils.hasText(triggerRef)) {
					Assert.state(!StringUtils.hasText(cron) && !StringUtils.hasText(fixedDelayValue)
									&& !StringUtils.hasText(fixedRateValue),
							"The '@Poller' 'trigger' attribute is mutually exclusive with other attributes.");
					trigger = this.beanFactory.getBean(triggerRef, Trigger.class);
				}
				else if (StringUtils.hasText(cron)) {
					Assert.state(!StringUtils.hasText(fixedDelayValue) && !StringUtils.hasText(fixedRateValue),
							"The '@Poller' 'cron' attribute is mutually exclusive with other attributes.");
					trigger = new CronTrigger(cron);
				}
				else if (StringUtils.hasText(fixedDelayValue)) {
					Assert.state(!StringUtils.hasText(fixedRateValue),
							"The '@Poller' 'fixedDelay' attribute is mutually exclusive with other attributes.");
					trigger = new PeriodicTrigger(Long.parseLong(fixedDelayValue));
				}
				else if (StringUtils.hasText(fixedRateValue)) {
					trigger = new PeriodicTrigger(Long.parseLong(fixedRateValue));
					((PeriodicTrigger) trigger).setFixedRate(true);
				}
				//'Trigger' can be null. 'PollingConsumer' does fallback to the 'new PeriodicTrigger(10)'.
				pollerMetadata.setTrigger(trigger);
			}


		}
		else {
			pollerMetadata = PollerMetadata.getDefaultPollerMetadata(this.beanFactory);
			Assert.notNull(pollerMetadata, "No poller has been defined for Annotation-based endpoint, " +
					"and no default poller is available within the context.");
		}
		pollingEndpoint.setTaskExecutor(pollerMetadata.getTaskExecutor());
		pollingEndpoint.setTrigger(pollerMetadata.getTrigger());
		pollingEndpoint.setAdviceChain(pollerMetadata.getAdviceChain());
		pollingEndpoint.setMaxMessagesPerPoll(pollerMetadata.getMaxMessagesPerPoll());
		pollingEndpoint.setErrorHandler(pollerMetadata.getErrorHandler());
		if (pollingEndpoint instanceof PollingConsumer) {
			((PollingConsumer) pollingEndpoint).setReceiveTimeout(pollerMetadata.getReceiveTimeout());
		}
		pollingEndpoint.setTransactionSynchronizationFactory(pollerMetadata.getTransactionSynchronizationFactory());
	}

	protected String generateHandlerBeanName(String originalBeanName, Method method) {
		String baseName = originalBeanName + "." + method.getName() + "."
				+ ClassUtils.getShortNameAsProperty(this.annotationType);
		String name = baseName;
		int count = 1;
		while (this.beanFactory.containsBean(name)) {
			name = baseName + "#" + (++count);
		}
		return name + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX;
	}

	protected void setOutputChannelIfPresent(List<Annotation> annotations, AbstractReplyProducingMessageHandler	handler) {
		String outputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, "outputChannel", String.class);
		if (StringUtils.hasText(outputChannelName)) {
			handler.setOutputChannel(this.channelResolver.resolveDestination(outputChannelName));
		}
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 *
	 * @param bean The bean.
	 * @param method The method.
	 * @return The MessageHandler.
	 */
	protected abstract MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations);

}
