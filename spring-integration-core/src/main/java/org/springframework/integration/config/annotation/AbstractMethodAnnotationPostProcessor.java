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

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
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


	protected final ConfigurableListableBeanFactory beanFactory;

	protected final ConversionService conversionService;

	protected final Environment environment;

	protected final DestinationResolver<MessageChannel> channelResolver;

	protected final Class<T> annotationType;

	@SuppressWarnings("unchecked")
	public AbstractMethodAnnotationPostProcessor(ListableBeanFactory beanFactory, Environment environment) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"'beanFactory' must be instanceOf ConfigurableListableBeanFactory");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		ConversionService conversionService = this.beanFactory.getConversionService();
		if (conversionService != null) {
			this.conversionService = conversionService;
		}
		else {
			this.conversionService = new DefaultConversionService();
		}
		this.environment = environment;
		this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
		this.annotationType = (Class<T>) GenericTypeResolver.resolveTypeArgument(this.getClass(),
				MethodAnnotationPostProcessor.class);
	}


	@Override
	public Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations) {
		MessageHandler handler = createHandler(bean, method, annotations);
		setAdviceChainIfPresent(beanName, annotations, handler);
		if (handler instanceof Orderable) {
			Order orderAnnotation = AnnotationUtils.findAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				((Orderable) handler).setOrder(orderAnnotation.value());
			}
		}

		boolean handlerExists = false;
		if (this.beanAnnotationAware() && AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			Object handlerBean = this.resolveTargetBeanFromMethodWithBeanAnnotation(method);
			handlerExists = handlerBean != null && handler == handlerBean;
		}

		if (!handlerExists) {
			String handlerBeanName = generateHandlerBeanName(beanName, method);
			this.beanFactory.registerSingleton(handlerBeanName, handler);
			handler = (MessageHandler) this.beanFactory.initializeBean(handler, handlerBeanName);
		}

		AbstractEndpoint endpoint = createEndpoint(handler, method, annotations);
		if (endpoint != null) {
			return endpoint;
		}
		return handler;
	}

	@Override
	public boolean shouldCreateEndpoint(Method method, List<Annotation> annotations) {
		String inputChannel = MessagingAnnotationUtils.resolveAttribute(annotations, getInputChannelAttribute(),
				String.class);
		boolean createEndpoint = StringUtils.hasText(inputChannel);
		if (!createEndpoint && beanAnnotationAware()) {
			boolean isBean = AnnotatedElementUtils.isAnnotated(method, Bean.class.getName());
			Assert.isTrue(!isBean, "A channel name in '" + getInputChannelAttribute() + "' is required when " + this.annotationType +
					" is used on '@Bean' methods.");
		}
		return createEndpoint;
	}

	protected String getInputChannelAttribute() {
		return INPUT_CHANNEL_ATTRIBUTE;
	}

	protected boolean beanAnnotationAware() {
		return true;
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

	protected AbstractEndpoint createEndpoint(MessageHandler handler, Method method, List<Annotation> annotations) {
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
				this.beanFactory.registerSingleton(inputChannelName, inputChannel);
				inputChannel = (MessageChannel) this.beanFactory.initializeBean(inputChannel, inputChannelName);
			}
			Assert.notNull(inputChannel, "failed to resolve inputChannel '" + inputChannelName + "'");

			endpoint = doCreateEndpoint(handler, inputChannel, annotations);
		}
		return endpoint;
	}

	protected AbstractEndpoint doCreateEndpoint(MessageHandler handler, MessageChannel inputChannel,List<Annotation> annotations) {
		AbstractEndpoint endpoint;
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
			handler.setOutputChannelName(outputChannelName);
		}
	}

	protected Object resolveTargetBeanFromMethodWithBeanAnnotation(Method method) {
		String id = null;
		String[] names = AnnotationUtils.getAnnotation(method, Bean.class).name();
		if (!ObjectUtils.isEmpty(names)) {
			id = names[0];
		}
		if (!StringUtils.hasText(id)) {
			id = method.getName();
		}
		return this.beanFactory.getBean(id);
	}

	@SuppressWarnings("unchecked")
	<H> H extractTypeIfPossible(Object targetObject, Class<H> expectedType) {
		if (targetObject == null) {
			return null;
		}
		if (expectedType.isAssignableFrom(targetObject.getClass())) {
			return (H) targetObject;
		}
		if (targetObject instanceof Advised) {
			TargetSource targetSource = ((Advised) targetObject).getTargetSource();
			if (targetSource == null) {
				return null;
			}
			try {
				return extractTypeIfPossible(targetSource.getTarget(), expectedType);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 *
	 * @param bean The bean.
	 * @param method The method.
	 * @param annotations The messaging annotation (or meta-annotation hierarchy) on the method.
	 * @return The MessageHandler.
	 */
	protected abstract MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations);

}
