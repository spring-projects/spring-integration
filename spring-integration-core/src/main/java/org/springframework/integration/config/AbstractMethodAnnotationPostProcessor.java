/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.integration.annotation.IdempotentReceiver;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.Reactive;
import org.springframework.integration.annotation.Role;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.config.annotation.MethodAnnotationPostProcessor;
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
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.handler.ReplyProducingMessageHandlerWrapper;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.util.ClassUtils;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.Message;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for Method-level annotation post-processors.
 *
 * @param <T> the target annotation type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 * @author Ngoc Nhan
 */
public abstract class AbstractMethodAnnotationPostProcessor<T extends Annotation>
		implements MethodAnnotationPostProcessor<T>, BeanFactoryAware {

	private static final String UNCHECKED = "unchecked";

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR

	protected static final String ADVICE_CHAIN_ATTRIBUTE = "adviceChain"; // NOSONAR

	protected static final String SEND_TIMEOUT_ATTRIBUTE = "sendTimeout"; // NOSONAR

	protected final List<String> messageHandlerAttributes = new ArrayList<>(); // NOSONAR

	protected final Class<T> annotationType;

	@SuppressWarnings("NullAway.Init")
	private ConfigurableListableBeanFactory beanFactory;

	@SuppressWarnings("NullAway.Init")
	private BeanDefinitionRegistry definitionRegistry;

	@SuppressWarnings("NullAway.Init")
	private ConversionService conversionService;

	@SuppressWarnings("NullAway.Init")
	private volatile DestinationResolver<MessageChannel> channelResolver;

	@SuppressWarnings("NullAway")
	public AbstractMethodAnnotationPostProcessor() {
		this.messageHandlerAttributes.add(SEND_TIMEOUT_ATTRIBUTE);
		this.annotationType =
				(Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), MethodAnnotationPostProcessor.class);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		this.definitionRegistry = (BeanDefinitionRegistry) beanFactory;
		this.conversionService =
				this.beanFactory.getConversionService() != null
						? this.beanFactory.getConversionService()
						: DefaultConversionService.getSharedInstance();
	}

	protected ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected BeanDefinitionRegistry getDefinitionRegistry() {
		return this.definitionRegistry;
	}

	protected ConversionService getConversionService() {
		return this.conversionService;
	}

	protected DestinationResolver<MessageChannel> getChannelResolver() {
		if (this.channelResolver == null) {
			this.channelResolver = ChannelResolverUtils.getChannelResolver(this.beanFactory);
		}
		return this.channelResolver;
	}

	@Override
	public void processBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition,
			List<Annotation> annotations) {

		ResolvableType handlerBeanType = getHandlerBeanClass(beanDefinition);

		String handlerBeanName = beanName;

		BeanDefinition handlerBeanDefinition =
				resolveHandlerBeanDefinition(beanName, beanDefinition, handlerBeanType, annotations);
		MergedAnnotations mergedAnnotations =
				Objects.requireNonNull(beanDefinition.getFactoryMethodMetadata()).getAnnotations();

		if (handlerBeanDefinition != null) {
			if (!handlerBeanDefinition.equals(beanDefinition)) {
				String beanClassName = handlerBeanDefinition.getBeanClassName();
				Assert.notNull(beanClassName, "No bean class present for " + handlerBeanDefinition);
				Class<?> handlerBeanClass =
						org.springframework.util.ClassUtils.resolveClassName(beanClassName,
								this.beanFactory.getBeanClassLoader());

				if (isClassIn(handlerBeanClass, Orderable.class, AbstractSimpleMessageHandlerFactoryBean.class)) {
					mergedAnnotations.get(Order.class)
							.getValue(AnnotationUtils.VALUE, String.class).
							ifPresent((order) -> handlerBeanDefinition.getPropertyValues().add("order", order));
				}

				if (isClassIn(handlerBeanClass, AbstractMessageProducingHandler.class, AbstractMessageRouter.class,
						AbstractSimpleMessageHandlerFactoryBean.class)) {

					new BeanDefinitionPropertiesMapper(handlerBeanDefinition, annotations)
							.setPropertyValue(SEND_TIMEOUT_ATTRIBUTE)
							.setPropertyValue("outputChannel", "outputChannelName")
							.setPropertyValue("defaultOutputChannel", "defaultOutputChannelName");
				}

				if (isClassIn(handlerBeanClass, AbstractMessageProducingHandler.class,
						AbstractSimpleMessageHandlerFactoryBean.class)
						&& !RouterFactoryBean.class.isAssignableFrom(handlerBeanClass)) {

					applyAdviceChainIfAny(handlerBeanDefinition, annotations);
				}

				handlerBeanName = generateHandlerBeanName(beanName, mergedAnnotations);
				this.definitionRegistry.registerBeanDefinition(handlerBeanName, handlerBeanDefinition);
			}
		}
		else {
			throw new BeanDefinitionValidationException(
					"The messaging annotation processor for '" + this.annotationType +
							"' cannot create a target handler bean. " +
							"The bean definition with the problem is: " + beanName);
		}

		BeanDefinition endpointBeanDefinition =
				createEndpointBeanDefinition(new BeanComponentDefinition(handlerBeanDefinition, handlerBeanName),
						new BeanComponentDefinition(beanDefinition, beanName), annotations);

		new BeanDefinitionPropertiesMapper(endpointBeanDefinition, annotations)
				.setPropertyValue("autoStartup")
				.setPropertyValue("phase");

		Poller poller = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller.class);
		Reactive reactive = MessagingAnnotationUtils.resolveAttribute(annotations, "reactive", Reactive.class);
		Assert.state(reactive == null || poller == null,
				() -> "The 'poller' and 'reactive' attributes are mutually exclusive." +
						"The bean definition with the problem is: " + beanName);
		if (poller != null) {
			applyPollerForEndpoint(endpointBeanDefinition, poller);
		}
		else if (reactive != null) {
			BeanMetadataElement reactiveCustomizer;
			String reactiveCustomizerBean = reactive.value();
			if (StringUtils.hasText(reactiveCustomizerBean)) {
				reactiveCustomizer = new RuntimeBeanReference(reactiveCustomizerBean);
			}
			else {
				reactiveCustomizer =
						BeanDefinitionBuilder.genericBeanDefinition(Function.class)
								.setFactoryMethod("identity")
								.getBeanDefinition();
			}
			endpointBeanDefinition.getPropertyValues().add("reactiveCustomizer", reactiveCustomizer);
		}

		mergedAnnotations.get(Role.class)
				.getValue(AnnotationUtils.VALUE, String.class).
				ifPresent((role) -> endpointBeanDefinition.getPropertyValues().add("role", role));

		String endpointBeanName =
				generateHandlerBeanName(beanName, mergedAnnotations)
						.replaceFirst("\\.(handler|source)$", "");
		this.beanFactory.registerDependentBean(beanName, endpointBeanName);
		this.definitionRegistry.registerBeanDefinition(endpointBeanName, endpointBeanDefinition);
	}

	private static boolean isClassIn(Class<?> classToVerify, Class<?>... classesToMatch) {
		for (Class<?> toMatch : classesToMatch) {
			if (toMatch.isAssignableFrom(classToVerify)) {
				return true;
			}
		}
		return false;
	}

	protected BeanDefinition createEndpointBeanDefinition(ComponentDefinition handlerBeanDefinition,
			ComponentDefinition beanDefinition, List<Annotation> annotations) {

		BeanDefinitionBuilder endpointDefinitionBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(ConsumerEndpointFactoryBean.class)
						.addPropertyReference("handler", handlerBeanDefinition.getName());

		String inputChannelAttribute = getInputChannelAttribute();
		String inputChannelName =
				MessagingAnnotationUtils.resolveAttribute(annotations, inputChannelAttribute, String.class);
		Assert.hasText(inputChannelName,
				() -> "The '" + inputChannelAttribute + "' attribute is required on '" + this.annotationType +
						"' on @Bean method");
		if (!this.definitionRegistry.containsBeanDefinition(inputChannelName)) {
			this.definitionRegistry.registerBeanDefinition(inputChannelName,
					new RootBeanDefinition(DirectChannel.class));
		}
		BeanDefinition endpointBeanDefinition =
				endpointDefinitionBuilder.addPropertyReference("inputChannel", inputChannelName)
						.getBeanDefinition();

		applyAdviceChainIfAny(endpointBeanDefinition, annotations);

		return endpointBeanDefinition;
	}

	private static void applyAdviceChainIfAny(BeanDefinition beanDefinition, List<Annotation> annotations) {
		String[] adviceChainNames = MessagingAnnotationUtils.resolveAttribute(annotations, ADVICE_CHAIN_ATTRIBUTE,
				String[].class);

		if (!ObjectUtils.isEmpty(adviceChainNames)) {
			ManagedList<RuntimeBeanReference> adviceChain =
					Arrays.stream(adviceChainNames)
							.map(RuntimeBeanReference::new)
							.collect(Collectors.toCollection(ManagedList::new));

			beanDefinition.getPropertyValues().addPropertyValue("adviceChain", adviceChain);
		}
	}

	private static void applyPollerForEndpoint(BeanDefinition endpointBeanDefinition, Poller poller) {
		BeanMetadataElement pollerMetadataBeanDefinition;
		String ref = poller.value();
		if (StringUtils.hasText(ref)) {
			pollerMetadataBeanDefinition = new RuntimeBeanReference(ref);
		}
		else {
			BeanDefinitionBuilder pollerBeanDefinitionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(PollerMetadata.class);

			new BeanDefinitionPropertiesMapper(pollerBeanDefinitionBuilder.getRawBeanDefinition(), List.of(poller))
					.setPropertyReference("trigger")
					.setPropertyReference("taskExecutor")
					.setPropertyValue("receiveTimeout")
					.setPropertyValue("maxMessagesPerPoll");

			String errorChannel = poller.errorChannel();
			if (StringUtils.hasText(errorChannel)) {
				BeanDefinitionBuilder errorHandler =
						BeanDefinitionBuilder.genericBeanDefinition(MessagePublishingErrorHandler.class)
								.addPropertyReference("defaultErrorChannel", errorChannel);
				pollerBeanDefinitionBuilder.addPropertyValue("errorHandler", errorHandler.getBeanDefinition());
			}

			String fixedDelay = poller.fixedDelay();
			String fixedRate = poller.fixedRate();
			String cron = poller.cron();
			BeanDefinition triggerBeanDefinition = null;

			if (StringUtils.hasText(cron)) {
				triggerBeanDefinition = triggerBeanDefinition(CronTrigger.class, cron);
			}
			else if (StringUtils.hasText(fixedDelay)) {
				triggerBeanDefinition = triggerBeanDefinition(PeriodicTrigger.class, fixedDelay);
			}
			else if (StringUtils.hasText(fixedRate)) {
				triggerBeanDefinition = triggerBeanDefinition(PeriodicTrigger.class, fixedRate);
				triggerBeanDefinition.getPropertyValues().addPropertyValue("fixedRate", true);
			}

			if (triggerBeanDefinition != null) {
				pollerBeanDefinitionBuilder.addPropertyValue("trigger", triggerBeanDefinition);
			}

			pollerMetadataBeanDefinition = pollerBeanDefinitionBuilder.getBeanDefinition();
		}

		endpointBeanDefinition.getPropertyValues()
				.addPropertyValue("pollerMetadata", pollerMetadataBeanDefinition);
	}

	private static BeanDefinition triggerBeanDefinition(Class<? extends Trigger> triggerClass, String triggerValue) {
		return BeanDefinitionBuilder.genericBeanDefinition(triggerClass)
				.addConstructorArgValue(triggerValue)
				.getBeanDefinition();
	}

	private ResolvableType getHandlerBeanClass(AnnotatedBeanDefinition beanDefinition) {
		MethodMetadata factoryMethodMetadata = beanDefinition.getFactoryMethodMetadata();
		if (factoryMethodMetadata instanceof StandardMethodMetadata standardMethodMetadata) {
			return ResolvableType.forMethodReturnType(standardMethodMetadata.getIntrospectedMethod());
		}
		else {
			Assert.notNull(factoryMethodMetadata, "No factoryMethodMetadata present for " + beanDefinition);
			String typeName = factoryMethodMetadata.getReturnTypeName();
			Class<?> beanClass =
					org.springframework.util.ClassUtils.resolveClassName(typeName,
							this.beanFactory.getBeanClassLoader());
			return ResolvableType.forClass(beanClass);
		}
	}

	@Nullable
	protected BeanDefinition resolveHandlerBeanDefinition(String beanName, AnnotatedBeanDefinition beanDefinition,
			ResolvableType handlerBeanType, List<Annotation> annotations) {

		Class<?> classToCheck = handlerBeanType.toClass();

		// Any out-of-the-box MessageHandler factory bean is considered as direct handler usage.
		if (AbstractSimpleMessageHandlerFactoryBean.class.isAssignableFrom(classToCheck)) {
			return beanDefinition;
		}

		if (FactoryBean.class.isAssignableFrom(classToCheck)) {
			classToCheck = this.beanFactory.getType(beanName);
		}
		Assert.state(classToCheck != null, "No handler bean found for " + beanName);
		if (isClassIn(classToCheck, AbstractMessageProducingHandler.class, AbstractMessageRouter.class)) {
			checkMessageHandlerAttributes(beanName, annotations);
			return beanDefinition;
		}

		return null;
	}

	@Override
	public Object postProcess(Object bean, String beanName, Method method, List<Annotation> annotations) {
		MessageHandler handler = createHandler(bean, method, annotations);

		if (!(handler instanceof ReactiveMessageHandlerAdapter)) {
			orderable(method, handler);
			producerOrRouter(annotations, handler);

			handler = registerHandlerBean(beanName, method, handler);

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
		String handlerBeanName = generateHandlerBeanName(beanName, method);
		if (handler instanceof ReplyProducingMessageHandlerWrapper
				&& StringUtils.hasText(MessagingAnnotationUtils.endpointIdValue(method))) {
			handlerBeanName = handlerBeanName + ".wrapper";
		}
		if (handler instanceof IntegrationObjectSupport integrationObjectSupport) {
			integrationObjectSupport.setComponentName(
					handlerBeanName.substring(0,
							handlerBeanName.indexOf(IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX)));
		}
		this.definitionRegistry.registerBeanDefinition(handlerBeanName,
				new RootBeanDefinition(MessageHandler.class, () -> handler));
		return this.beanFactory.getBean(handlerBeanName, MessageHandler.class);
	}

	private void producerOrRouter(List<Annotation> annotations, MessageHandler handler) {
		if (handler instanceof AbstractMessageProducingHandler || handler instanceof AbstractMessageRouter) {
			String sendTimeout = MessagingAnnotationUtils.resolveAttribute(annotations, "sendTimeout", String.class);
			if (sendTimeout != null) {
				String resolvedValue = this.beanFactory.resolveEmbeddedValue(sendTimeout);
				if (resolvedValue != null) {
					long value = Long.parseLong(resolvedValue);
					if (handler instanceof AbstractMessageProducingHandler abstractMessageProducingHandler) {
						abstractMessageProducingHandler.setSendTimeout(value);
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
					Objects.requireNonNull(AnnotationUtils.getAnnotation(method, IdempotentReceiver.class)).value();
			for (String interceptor : interceptors) {
				DefaultBeanFactoryPointcutAdvisor advisor = new DefaultBeanFactoryPointcutAdvisor();
				advisor.setAdviceBeanName(interceptor);
				NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
				pointcut.setMappedName("handleMessage");
				advisor.setPointcut(pointcut);
				advisor.setBeanFactory(this.beanFactory);

				if (handler instanceof Advised advised) {
					advised.addAdvisor(advisor);
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

		if (!CollectionUtils.isEmpty(adviceChain) && handler instanceof AbstractReplyProducingMessageHandler abstractReplyProducingMessageHandler) {
			abstractReplyProducingMessageHandler.setAdviceChain(adviceChain);
		}

		if (!CollectionUtils.isEmpty(adviceChain)) {
			for (Advice advice : adviceChain) {
				if (advice instanceof HandleMessageAdvice) {
					NameMatchMethodPointcutAdvisor handlerAdvice = new NameMatchMethodPointcutAdvisor(advice);
					handlerAdvice.addMethodName("handleMessage");
					if (handler instanceof Advised advised) {
						advised.addAdvisor(handlerAdvice);
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

	protected @Nullable List<Advice> extractAdviceChain(String beanName, List<Annotation> annotations) {
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
				if (adviceChainBean instanceof Advice advice) {
					adviceChain.add(advice);
				}
				else if (adviceChainBean instanceof Advice[] advices) {
					Collections.addAll(adviceChain, advices);
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

	protected @Nullable AbstractEndpoint createEndpoint(MessageHandler handler, @SuppressWarnings("unused") Method method,
			List<Annotation> annotations) {

		AbstractEndpoint endpoint = null;
		String inputChannelName = MessagingAnnotationUtils.resolveAttribute(annotations, getInputChannelAttribute(),
				String.class);
		if (StringUtils.hasText(inputChannelName)) {
			MessageChannel inputChannel;
			try {
				inputChannel = getChannelResolver().resolveDestination(inputChannelName);
			}
			catch (DestinationResolutionException e) {
				if (e.getCause() instanceof NoSuchBeanDefinitionException) {
					this.definitionRegistry.registerBeanDefinition(inputChannelName,
							new RootBeanDefinition(DirectChannel.class, DirectChannel::new));
					inputChannel = this.beanFactory.getBean(inputChannelName, MessageChannel.class);
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

		Poller poller = MessagingAnnotationUtils.resolveAttribute(annotations, "poller", Poller.class);

		Reactive reactive = MessagingAnnotationUtils.resolveAttribute(annotations, "reactive", Reactive.class);

		Assert.state(reactive == null || poller == null,
				"The 'poller' and 'reactive' are mutually exclusive.");

		if (inputChannel instanceof Publisher || handler instanceof ReactiveMessageHandlerAdapter || reactive != null) {
			return reactiveStreamsConsumer(inputChannel, handler, reactive);
		}
		else if (inputChannel instanceof SubscribableChannel subscribableChannel) {
			Assert.state(poller == null, () ->
					"A '@Poller' should not be specified for Annotation-based " +
							"endpoint, since '" + inputChannel + "' is a SubscribableChannel (not pollable).");
			return new EventDrivenConsumer(subscribableChannel, handler);
		}
		else if (inputChannel instanceof PollableChannel) {
			return pollingConsumer(inputChannel, handler, poller);
		}
		else {
			throw new IllegalArgumentException("Unsupported 'inputChannel' type: '"
					+ inputChannel.getClass().getName() + "'. " +
					"Must be one of 'SubscribableChannel', 'PollableChannel' or 'ReactiveStreamsSubscribableChannel'");
		}
	}

	private ReactiveStreamsConsumer reactiveStreamsConsumer(MessageChannel channel, MessageHandler handler,
			@Nullable Reactive reactive) {

		ReactiveStreamsConsumer reactiveStreamsConsumer;
		if (handler instanceof ReactiveMessageHandlerAdapter reactiveMessageHandlerAdapter) {
			reactiveStreamsConsumer = new ReactiveStreamsConsumer(channel,
				reactiveMessageHandlerAdapter.getDelegate());
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

	private PollingConsumer pollingConsumer(MessageChannel inputChannel, MessageHandler handler, @Nullable Poller poller) {
		PollingConsumer pollingConsumer = new PollingConsumer((PollableChannel) inputChannel, handler);
		configurePollingEndpoint(pollingConsumer, poller);
		return pollingConsumer;
	}

	protected void configurePollingEndpoint(AbstractPollingEndpoint pollingEndpoint, @Nullable Poller poller) {
		PollerMetadata pollerMetadata;
		if (poller != null) {
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
		long maxMessagesPerPoll = pollerMetadata.getMaxMessagesPerPoll();
		if (maxMessagesPerPoll == PollerMetadata.MAX_MESSAGES_UNBOUNDED &&
				pollingEndpoint instanceof SourcePollingChannelAdapter) {
			// the default is 1 since a source might return
			// a non-null and non-interruptible value every time it is invoked
			maxMessagesPerPoll = 1;
		}
		pollingEndpoint.setMaxMessagesPerPoll(maxMessagesPerPoll);
		pollingEndpoint.setErrorHandler(pollerMetadata.getErrorHandler());
		if (pollingEndpoint instanceof PollingConsumer pollingConsumer) {
			pollingConsumer.setReceiveTimeout(pollerMetadata.getReceiveTimeout());
		}
		pollingEndpoint.setTransactionSynchronizationFactory(pollerMetadata.getTransactionSynchronizationFactory());
	}

	private PollerMetadata configurePoller(AbstractPollingEndpoint pollingEndpoint, String triggerRef,
			String executorRef, @Nullable String fixedDelayValue, @Nullable String fixedRateValue,
			@Nullable String maxMessagesPerPollValue, @Nullable String cron, @Nullable String errorChannel,
			@Nullable String receiveTimeout) {

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

	private void trigger(String triggerRef, @Nullable String fixedDelayValue, @Nullable String fixedRateValue, @Nullable String cron,
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
		else if (StringUtils.hasText(fixedDelayValue) || StringUtils.hasText(fixedRateValue)) {
			PeriodicTriggerFactoryBean periodicTriggerFactoryBean = new PeriodicTriggerFactoryBean();
			periodicTriggerFactoryBean.setFixedDelayValue(fixedDelayValue);
			periodicTriggerFactoryBean.setFixedRateValue(fixedRateValue);
			trigger = periodicTriggerFactoryBean.getObject();
		}
		//'Trigger' can be null. 'PollingConsumer' does fallback to the 'new PeriodicTrigger(10)'.
		pollerMetadata.setTrigger(trigger);
	}

	protected String generateHandlerBeanName(String originalBeanName, Method method) {
		return generateHandlerBeanName(originalBeanName, MergedAnnotations.from(method), method.getName());
	}

	protected String generateHandlerBeanName(String originalBeanName, MergedAnnotations mergedAnnotations) {
		return generateHandlerBeanName(originalBeanName, mergedAnnotations, null);
	}

	protected String generateHandlerBeanName(String originalBeanName, MergedAnnotations mergedAnnotations,
			@Nullable String methodName) {

		String name = MessagingAnnotationUtils.endpointIdValue(mergedAnnotations);
		if (!StringUtils.hasText(name)) {
			String baseName = originalBeanName + (methodName != null ? "." + methodName : "") + "."
					+ org.springframework.util.ClassUtils.getShortNameAsProperty(this.annotationType);
			name = baseName;
			int count = 1;
			while (this.beanFactory.containsBean(name)) {
				name = baseName + '#' + (++count);
			}
		}
		return name + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX;
	}

	protected static void setOutputChannelIfPresent(List<Annotation> annotations,
			AbstractMessageProducingHandler handler) {

		String outputChannel = MessagingAnnotationUtils.resolveAttribute(annotations, "outputChannel", String.class);
		if (StringUtils.hasText(outputChannel)) {
			handler.setOutputChannelName(outputChannel);
		}
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

	protected static @Nullable BeanDefinition buildLambdaMessageProcessor(ResolvableType beanType,
			AnnotatedBeanDefinition beanDefinition) {

		Class<?> beanClass = beanType.toClass();

		if (Function.class.isAssignableFrom(beanClass)
				|| Consumer.class.isAssignableFrom(beanClass)
				|| ClassUtils.isKotlinFunction1(beanClass)) {

			Class<?> expectedPayloadType = beanType.getGeneric(0).toClass();
			return BeanDefinitionBuilder.genericBeanDefinition(LambdaMessageProcessor.class)
					.addConstructorArgValue(beanDefinition)
					.addConstructorArgValue(expectedPayloadType)
					.getBeanDefinition();
		}

		return null;
	}

	private static void orderable(Method method, MessageHandler handler) {
		if (handler instanceof Orderable orderable) {
			Order orderAnnotation = AnnotationUtils.findAnnotation(method, Order.class);
			if (orderAnnotation != null) {
				orderable.setOrder(orderAnnotation.value());
			}
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

	protected record BeanDefinitionPropertiesMapper(BeanDefinition beanDefinition, List<Annotation> annotations) {

		public BeanDefinitionPropertiesMapper setPropertyValue(String property) {
			return setPropertyValue(property, null);
		}

		public BeanDefinitionPropertiesMapper setPropertyValue(String property, @Nullable String propertyName) {
			String value = MessagingAnnotationUtils.resolveAttribute(this.annotations, property, String.class);
			if (StringUtils.hasText(value)) {
				this.beanDefinition.getPropertyValues().add(propertyName != null ? propertyName : property, value);
			}
			return this;
		}

		public BeanDefinitionPropertiesMapper setPropertyReference(String property) {
			return setPropertyReference(property, null);
		}

		public BeanDefinitionPropertiesMapper setPropertyReference(String property, @Nullable String propertyName) {
			String value = MessagingAnnotationUtils.resolveAttribute(this.annotations, property, String.class);
			if (StringUtils.hasText(value)) {
				this.beanDefinition.getPropertyValues()
						.add(propertyName != null ? propertyName : property, new RuntimeBeanReference(value));
			}
			return this;
		}

	}

}
