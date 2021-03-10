/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.function.Consumer;
import java.util.function.Function;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.annotation.Bean;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.annotation.IdempotentReceiver;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Reactive;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.LambdaMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.handler.ReplyProducingMessageHandlerWrapper;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.util.ClassUtils;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

/**
 * Base class for Method-level annotation post-processors.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class AbstractMethodAnnotationPostProcessor<T extends Annotation>
		implements MethodAnnotationPostProcessor<T> {

	private static final String UNCHECKED = "unchecked";

	private static final String INPUT_CHANNEL_ATTRIBUTE = "inputChannel";

	private static final String ADVICE_CHAIN_ATTRIBUTE = "adviceChain";

	protected static final String SEND_TIMEOUT_ATTRIBUTE = "sendTimeout";

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR

	protected final List<String> messageHandlerAttributes = new ArrayList<>(); // NOSONAR

	protected final ConfigurableListableBeanFactory beanFactory; // NOSONAR

	protected final ConversionService conversionService; // NOSONAR

	protected final DestinationResolver<MessageChannel> channelResolver; // NOSONAR

	protected final Class<T> annotationType; // NOSONAR

	protected final Disposables disposables; // NOSONAR

	@SuppressWarnings(UNCHECKED)
	public AbstractMethodAnnotationPostProcessor(ConfigurableListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		this.messageHandlerAttributes.add(SEND_TIMEOUT_ATTRIBUTE);
		this.beanFactory = beanFactory;
		this.conversionService = this.beanFactory.getConversionService() != null
				? this.beanFactory.getConversionService()
				: DefaultConversionService.getSharedInstance();
		this.channelResolver = ChannelResolverUtils.getChannelResolver(beanFactory);
		this.annotationType =
				(Class<T>) GenericTypeResolver.resolveTypeArgument(this.getClass(),
						MethodAnnotationPostProcessor.class);
		Disposables disposablesBean = null;
		try {
			disposablesBean = beanFactory.getBean(Disposables.class);
		}
		catch (@SuppressWarnings("unused") Exception e) {
			// NOSONAR - only for test cases
		}
		this.disposables = disposablesBean;
	}

	@Override
	public Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations) {
		Object sourceHandler = null;
		if (beanAnnotationAware() && AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {
			if (!this.beanFactory.containsBeanDefinition(resolveTargetBeanName(method))) {
				this.logger.debug("Skipping endpoint creation; perhaps due to some '@Conditional' annotation.");
				return null;
			}
			else {
				sourceHandler = resolveTargetBeanFromMethodWithBeanAnnotation(method);
			}
		}

		MessageHandler handler = createHandler(bean, method, annotations);

		if (!(handler instanceof ReactiveMessageHandlerAdapter)) {
			orderable(method, handler);
			producerOrRouter(annotations, handler);

			if (!handler.equals(sourceHandler)) {
				handler = registerHandlerBean(beanName, method, handler);
			}

			handler = annotated(method, handler);
			handler = adviceChain(beanName, annotations, handler);
		}

		AbstractEndpoint endpoint = createEndpoint(handler, method, annotations);
		if (endpoint != null) {
			return endpoint;
		}
		else {
			return handler;
		}
	}

	private MessageHandler registerHandlerBean(String beanName, Method method, final MessageHandler handler) {
		MessageHandler handlerBean = handler;
		String handlerBeanName = generateHandlerBeanName(beanName, method);
		if (handlerBean instanceof ReplyProducingMessageHandlerWrapper
				&& StringUtils.hasText(MessagingAnnotationUtils.endpointIdValue(method))) {
			handlerBeanName = handlerBeanName + ".wrapper";
		}
		if (handlerBean instanceof IntegrationObjectSupport) {
			((IntegrationObjectSupport) handlerBean).setComponentName(
					handlerBeanName.substring(0,
							handlerBeanName.indexOf(IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX)));
		}
		this.beanFactory.registerSingleton(handlerBeanName, handler);
		handlerBean = (MessageHandler) this.beanFactory.initializeBean(handlerBean, handlerBeanName);
		if (handlerBean instanceof DisposableBean && this.disposables != null) {
			this.disposables.add((DisposableBean) handlerBean);
		}
		return handlerBean;
	}

	private void orderable(Method method, MessageHandler handler) {
		if (handler instanceof Orderable) {
			Order orderAnnotation = AnnotationUtils.findAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				((Orderable) handler).setOrder(orderAnnotation.value());
			}
		}
	}

	private void producerOrRouter(List<Annotation> annotations, MessageHandler handler) {
		if (handler instanceof AbstractMessageProducingHandler || handler instanceof AbstractMessageRouter) {
			String sendTimeout = MessagingAnnotationUtils.resolveAttribute(annotations, "sendTimeout", String.class);
			if (sendTimeout != null) {
				String resolvedValue = this.beanFactory.resolveEmbeddedValue(sendTimeout);
				if (resolvedValue != null) {
					long value = Long.parseLong(resolvedValue);
					if (handler instanceof AbstractMessageProducingHandler) {
						((AbstractMessageProducingHandler) handler).setSendTimeout(value);
					}
					else {
						((AbstractMessageRouter) handler).setSendTimeout(value);
					}
				}
			}
		}
	}

	private MessageHandler annotated(Method method, MessageHandler handlerArg) {
		MessageHandler handler = handlerArg;
		if (AnnotatedElementUtils.isAnnotated(method, IdempotentReceiver.class.getName())
				&& !AnnotatedElementUtils.isAnnotated(method, Bean.class.getName())) {

			String[] interceptors =
					AnnotationUtils.getAnnotation(method, IdempotentReceiver.class).value(); // NOSONAR never null
			for (String interceptor : interceptors) {
				DefaultBeanFactoryPointcutAdvisor advisor = new DefaultBeanFactoryPointcutAdvisor();
				advisor.setAdviceBeanName(interceptor);
				NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
				pointcut.setMappedName("handleMessage");
				advisor.setPointcut(pointcut);
				advisor.setBeanFactory(this.beanFactory);

				if (handler instanceof Advised) {
					((Advised) handler).addAdvisor(advisor);
				}
				else {
					ProxyFactory proxyFactory = new ProxyFactory(handler);
					proxyFactory.addAdvisor(advisor);
					handler = (MessageHandler) proxyFactory.getProxy(this.beanFactory.getBeanClassLoader());
				}
			}
		}
		return handler;
	}

	private MessageHandler adviceChain(String beanName, List<Annotation> annotations, MessageHandler handlerArg) {
		MessageHandler handler = handlerArg;
		List<Advice> adviceChain = extractAdviceChain(beanName, annotations);

		if (!CollectionUtils.isEmpty(adviceChain) && handler instanceof AbstractReplyProducingMessageHandler) {
			((AbstractReplyProducingMessageHandler) handler).setAdviceChain(adviceChain);
		}

		if (!CollectionUtils.isEmpty(adviceChain)) {
			for (Advice advice : adviceChain) {
				if (advice instanceof HandleMessageAdvice) {
					NameMatchMethodPointcutAdvisor handlerAdvice = new NameMatchMethodPointcutAdvisor(advice);
					handlerAdvice.addMethodName("handleMessage");
					if (handler instanceof Advised) {
						((Advised) handler).addAdvisor(handlerAdvice);
					}
					else {
						ProxyFactory proxyFactory = new ProxyFactory(handler);
						proxyFactory.addAdvisor(handlerAdvice);
						handler = (MessageHandler) proxyFactory.getProxy(this.beanFactory.getBeanClassLoader());
					}
				}
			}
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
			Assert.isTrue(!isBean, () -> "A channel name in '" + getInputChannelAttribute() + "' is required when " +
					this.annotationType + " is used on '@Bean' methods.");
		}
		return createEndpoint;
	}

	protected String getInputChannelAttribute() {
		return INPUT_CHANNEL_ATTRIBUTE;
	}

	protected boolean beanAnnotationAware() {
		return true;
	}

	protected List<Advice> extractAdviceChain(String beanName, List<Annotation> annotations) {
		List<Advice> adviceChain = null;
		String[] adviceChainNames = MessagingAnnotationUtils.resolveAttribute(annotations, ADVICE_CHAIN_ATTRIBUTE,
				String[].class);
		/*
		 * Note: we don't merge advice chain contents; if the directAnnotation has a non-empty
		 * attribute, it wins. You cannot "remove" an advice chain from a meta-annotation
		 * by setting an empty array on the custom annotation.
		 */
		if (adviceChainNames != null && adviceChainNames.length > 0) {
			adviceChain = new ArrayList<>();
			for (String adviceChainName : adviceChainNames) {
				Object adviceChainBean = this.beanFactory.getBean(adviceChainName);
				if (adviceChainBean instanceof Advice) {
					adviceChain.add((Advice) adviceChainBean);
				}
				else if (adviceChainBean instanceof Advice[]) {
					Collections.addAll(adviceChain, (Advice[]) adviceChainBean);
				}
				else if (adviceChainBean instanceof Collection) {
					@SuppressWarnings(UNCHECKED)
					Collection<Advice> adviceChainEntries = (Collection<Advice>) adviceChainBean;
					adviceChain.addAll(adviceChainEntries);
				}
				else {
					throw new IllegalArgumentException("Invalid advice chain type:" +
							adviceChainName.getClass().getName() + " for bean '" + beanName + "'");
				}
			}
		}
		return adviceChain;
	}

	protected AbstractEndpoint createEndpoint(MessageHandler handler, @SuppressWarnings("unused") Method method,
			List<Annotation> annotations) {

		AbstractEndpoint endpoint = null;
		String inputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, getInputChannelAttribute(),
				String.class);
		if (StringUtils.hasText(inputChannelName)) {
			MessageChannel inputChannel;
			try {
				inputChannel = this.channelResolver.resolveDestination(inputChannelName);
			}
			catch (DestinationResolutionException e) {
				if (e.getCause() instanceof NoSuchBeanDefinitionException) {
					inputChannel = new DirectChannel();
					this.beanFactory.registerSingleton(inputChannelName, inputChannel);
					inputChannel = (MessageChannel) this.beanFactory.initializeBean(inputChannel, inputChannelName);
					if (this.disposables != null) {
						this.disposables.add((DisposableBean) inputChannel);
					}
				}
				else {
					throw e;
				}
			}
			Assert.notNull(inputChannel, () -> "failed to resolve inputChannel '" + inputChannelName + "'");

			endpoint = doCreateEndpoint(handler, inputChannel, annotations);
		}
		return endpoint;
	}

	protected AbstractEndpoint doCreateEndpoint(MessageHandler handler, MessageChannel inputChannel,
			List<Annotation> annotations) {

		Poller[] pollers = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller[].class);
		Reactive reactive = MessagingAnnotationUtils.resolveAttribute(annotations, "reactive", Reactive.class);
		boolean reactiveProvided = reactive != null && !ValueConstants.DEFAULT_NONE.equals(reactive.value());

		Assert.state(!reactiveProvided || ObjectUtils.isEmpty(pollers),
				"The 'poller' and 'reactive' are mutually exclusive.");

		if (inputChannel instanceof Publisher || handler instanceof ReactiveMessageHandlerAdapter || reactiveProvided) {
			return reactiveStreamsConsumer(inputChannel, handler, reactiveProvided ? reactive : null);
		}
		else if (inputChannel instanceof SubscribableChannel) {
			Assert.state(ObjectUtils.isEmpty(pollers), () ->
					"A '@Poller' should not be specified for Annotation-based " +
							"endpoint, since '" + inputChannel + "' is a SubscribableChannel (not pollable).");
			return new EventDrivenConsumer((SubscribableChannel) inputChannel, handler);
		}
		else if (inputChannel instanceof PollableChannel) {
			return pollingConsumer(inputChannel, handler, pollers);
		}
		else {
			throw new IllegalArgumentException("Unsupported 'inputChannel' type: '"
					+ inputChannel.getClass().getName() + "'. " +
					"Must be one of 'SubscribableChannel', 'PollableChannel' or 'ReactiveStreamsSubscribableChannel'");
		}
	}

	private ReactiveStreamsConsumer reactiveStreamsConsumer(MessageChannel channel, MessageHandler handler,
			Reactive reactive) {

		ReactiveStreamsConsumer reactiveStreamsConsumer;
		if (handler instanceof ReactiveMessageHandlerAdapter) {
			reactiveStreamsConsumer = new ReactiveStreamsConsumer(channel,
					((ReactiveMessageHandlerAdapter) handler).getDelegate());
		}
		else {
			reactiveStreamsConsumer = new ReactiveStreamsConsumer(channel, handler);
		}

		if (reactive != null) {
			String functionBeanName = reactive.value();
			if (StringUtils.hasText(functionBeanName)) {
				@SuppressWarnings(UNCHECKED)
				Function<? super Flux<Message<?>>, ? extends Publisher<Message<?>>> reactiveCustomizer =
						this.beanFactory.getBean(functionBeanName, Function.class);
				reactiveStreamsConsumer.setReactiveCustomizer(reactiveCustomizer);
			}
		}

		return reactiveStreamsConsumer;
	}

	private PollingConsumer pollingConsumer(MessageChannel inputChannel, MessageHandler handler, Poller[] pollers) {
		PollingConsumer pollingConsumer = new PollingConsumer((PollableChannel) inputChannel, handler);
		configurePollingEndpoint(pollingConsumer, pollers);
		return pollingConsumer;
	}

	protected void configurePollingEndpoint(AbstractPollingEndpoint pollingEndpoint, Poller[] pollers) {
		PollerMetadata pollerMetadata;
		if (!ObjectUtils.isEmpty(pollers)) {
			Assert.state(pollers.length == 1,
					"The 'poller' for an Annotation-based endpoint can have only one '@Poller'.");
			Poller poller = pollers[0];

			String ref = poller.value();
			String triggerRef = poller.trigger();
			String executorRef = poller.taskExecutor();
			String fixedDelayValue = this.beanFactory.resolveEmbeddedValue(poller.fixedDelay());
			String fixedRateValue = this.beanFactory.resolveEmbeddedValue(poller.fixedRate());
			String maxMessagesPerPollValue = this.beanFactory.resolveEmbeddedValue(poller.maxMessagesPerPoll());
			String cron = this.beanFactory.resolveEmbeddedValue(poller.cron());
			String errorChannel = this.beanFactory.resolveEmbeddedValue(poller.errorChannel());
			String receiveTimeout = this.beanFactory.resolveEmbeddedValue(poller.receiveTimeout());

			if (StringUtils.hasText(ref)) {
				Assert.state(!StringUtils.hasText(triggerRef)
								&& !StringUtils.hasText(executorRef)
								&& !StringUtils.hasText(cron)
								&& !StringUtils.hasText(fixedDelayValue)
								&& !StringUtils.hasText(fixedRateValue)
								&& !StringUtils.hasText(maxMessagesPerPollValue), // NOSONAR boolean complexity
						"The '@Poller' 'ref' attribute is mutually exclusive with other attributes.");
				pollerMetadata = this.beanFactory.getBean(ref, PollerMetadata.class);
			}
			else {
				pollerMetadata = configurePoller(pollingEndpoint, triggerRef, executorRef, fixedDelayValue,
						fixedRateValue, maxMessagesPerPollValue, cron, errorChannel, receiveTimeout);
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


	private PollerMetadata configurePoller(AbstractPollingEndpoint pollingEndpoint, String triggerRef,
			String executorRef, String fixedDelayValue, String fixedRateValue, String maxMessagesPerPollValue,
			String cron, String errorChannel, String receiveTimeout) {

		PollerMetadata pollerMetadata;
		pollerMetadata = new PollerMetadata();
		if (StringUtils.hasText(maxMessagesPerPollValue)) {
			pollerMetadata.setMaxMessagesPerPoll(Long.parseLong(maxMessagesPerPollValue));
		}
		else if (pollingEndpoint instanceof SourcePollingChannelAdapter) {
			// SPCAs default to 1 message per poll
			pollerMetadata.setMaxMessagesPerPoll(1);
		}

		if (StringUtils.hasText(executorRef)) {
			pollerMetadata.setTaskExecutor(this.beanFactory.getBean(executorRef, TaskExecutor.class));
		}

		trigger(triggerRef, fixedDelayValue, fixedRateValue, cron, pollerMetadata);

		if (StringUtils.hasText(errorChannel)) {
			MessagePublishingErrorHandler errorHandler = new MessagePublishingErrorHandler();
			errorHandler.setDefaultErrorChannelName(errorChannel);
			errorHandler.setBeanFactory(this.beanFactory);
			pollerMetadata.setErrorHandler(errorHandler);
		}

		if (StringUtils.hasText(receiveTimeout)) {
			pollerMetadata.setReceiveTimeout(Long.parseLong(receiveTimeout));
		}
		return pollerMetadata;
	}

	private void trigger(String triggerRef, String fixedDelayValue, String fixedRateValue, String cron,
			PollerMetadata pollerMetadata) {

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

	protected String generateHandlerBeanName(String originalBeanName, Method method) {
		String name = MessagingAnnotationUtils.endpointIdValue(method);
		if (!StringUtils.hasText(name)) {
			String baseName = originalBeanName + "." + method.getName() + "."
					+ org.springframework.util.ClassUtils.getShortNameAsProperty(this.annotationType);
			name = baseName;
			int count = 1;
			while (this.beanFactory.containsBean(name)) {
				name = baseName + '#' + (++count);
			}
		}
		return name + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX;
	}

	protected void setOutputChannelIfPresent(List<Annotation> annotations,
			AbstractReplyProducingMessageHandler handler) {
		String outputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, "outputChannel",
				String.class);
		if (StringUtils.hasText(outputChannelName)) {
			handler.setOutputChannelName(outputChannelName);
		}
	}

	protected Object resolveTargetBeanFromMethodWithBeanAnnotation(Method method) {
		String id = resolveTargetBeanName(method);
		return this.beanFactory.getBean(id);
	}

	protected String resolveTargetBeanName(Method method) {
		String id = method.getName();
		String[] names = AnnotationUtils.getAnnotation(method, Bean.class).name(); // NOSONAR never null
		if (!ObjectUtils.isEmpty(names)) {
			id = names[0];
		}
		return id;
	}

	@SuppressWarnings(UNCHECKED)
	protected <H> H extractTypeIfPossible(@Nullable Object targetObject, Class<H> expectedType) {
		if (targetObject == null) {
			return null;
		}
		if (expectedType.isAssignableFrom(targetObject.getClass())) {
			return (H) targetObject;
		}
		if (targetObject instanceof Advised) {
			TargetSource targetSource = ((Advised) targetObject).getTargetSource();
			try {
				return extractTypeIfPossible(targetSource.getTarget(), expectedType);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}

	protected void checkMessageHandlerAttributes(String handlerBeanName, List<Annotation> annotations) {
		for (String attribute : this.messageHandlerAttributes) {
			for (Annotation annotation : annotations) {
				Object value = AnnotationUtils.getValue(annotation, attribute);
				if (MessagingAnnotationUtils.hasValue(value)) {
					throw new BeanDefinitionValidationException("The MessageHandler [" + handlerBeanName +
							"] can not be populated because of ambiguity with annotation attributes " +
							this.messageHandlerAttributes + " which are not allowed when an integration annotation " +
							"is used with a @Bean definition for a MessageHandler." +
							"\nThe attribute causing the ambiguity is: [" + attribute + "]." +
							"\nUse the appropriate setter on the MessageHandler directly when configuring an " +
							"endpoint this way.");
				}
			}
		}
	}

	protected boolean resolveAttributeToBoolean(String attribute) {
		return Boolean.parseBoolean(this.beanFactory.resolveEmbeddedValue(attribute));
	}

	@Nullable
	protected MessageProcessor<?> buildLambdaMessageProcessorForBeanMethod(Method method, Object target) {
		if ((target instanceof Function || target instanceof Consumer) && ClassUtils.isLambda(target.getClass())
				|| ClassUtils.isKotlinFaction1(target.getClass())) {

			ResolvableType methodReturnType = ResolvableType.forMethodReturnType(method);
			Class<?> expectedPayloadType = methodReturnType.getGeneric(0).toClass();
			return new LambdaMessageProcessor(target, expectedPayloadType);
		}
		else {
			return null;
		}
	}

	/**
	 * Subclasses must implement this method to create the MessageHandler.
	 * @param bean The bean.
	 * @param method The method.
	 * @param annotations The messaging annotation (or meta-annotation hierarchy) on the method.
	 * @return The MessageHandler.
	 */
	protected abstract MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations);

}
