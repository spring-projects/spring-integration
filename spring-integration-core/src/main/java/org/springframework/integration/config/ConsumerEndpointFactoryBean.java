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

package org.springframework.integration.config;

import java.util.List;
import java.util.function.Function;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;


/**
 * The {@link FactoryBean} implementation for {@link AbstractEndpoint} population.
 * Controls all the necessary properties and lifecycle.
 * According the provided {@link MessageChannel} implementation populates
 * a {@link PollingConsumer} for the {@link PollableChannel},
 * an {@link EventDrivenConsumer} for the {@link SubscribableChannel}
 * and {@link ReactiveStreamsConsumer} for all other channel implementations.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Josh Long
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ConsumerEndpointFactoryBean
		implements FactoryBean<AbstractEndpoint>, BeanFactoryAware, BeanNameAware, BeanClassLoaderAware,
		InitializingBean, SmartLifecycle, DisposableBean {

	private static final LogAccessor LOGGER = new LogAccessor(LogFactory.getLog(ConsumerEndpointFactoryBean.class));

	private final Object initializationMonitor = new Object();

	private final Object handlerMonitor = new Object();

	private MessageHandler handler;

	private String beanName;

	private String inputChannelName;

	private PollerMetadata pollerMetadata;

	@Nullable
	private Function<? super Flux<Message<?>>, ? extends Publisher<Message<?>>> reactiveCustomizer;

	private Boolean autoStartup;

	private int phase = 0;

	private boolean isPhaseSet;

	private String role;

	private MessageChannel inputChannel;

	private ConfigurableBeanFactory beanFactory;

	private ClassLoader beanClassLoader;

	private List<Advice> adviceChain;

	private DestinationResolver<MessageChannel> channelResolver;

	private TaskScheduler taskScheduler;

	private volatile AbstractEndpoint endpoint;

	private volatile boolean initialized;

	public void setHandler(Object handler) {
		Assert.isTrue(handler instanceof MessageHandler || handler instanceof ReactiveMessageHandler,
				"'handler' must be an instance of 'MessageHandler' or 'ReactiveMessageHandler'");
		synchronized (this.handlerMonitor) {
			Assert.isNull(this.handler, "handler cannot be overridden");
			if (handler instanceof ReactiveMessageHandler) {
				this.handler = new ReactiveMessageHandlerAdapter((ReactiveMessageHandler) handler);
			}
			else {
				this.handler = (MessageHandler) handler;
			}
		}
	}

	public void setInputChannel(MessageChannel inputChannel) {
		this.inputChannel = inputChannel;
	}

	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.pollerMetadata = pollerMetadata;
	}

	public void setReactiveCustomizer(
			@Nullable Function<? super Flux<Message<?>>, ? extends Publisher<Message<?>>> reactiveCustomizer) {

		this.reactiveCustomizer = reactiveCustomizer;
	}

	/**
	 * Specify the {@link DestinationResolver} strategy to use.
	 * The default is a BeanFactoryChannelResolver.
	 * @param channelResolver The channel resolver.
	 * @since 4.1.3
	 */
	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.channelResolver = channelResolver;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setAutoStartup(Boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
		this.isPhaseSet = true;
	}

	public void setRole(String role) {
		this.role = role;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory, "a ConfigurableBeanFactory is required");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		Assert.notNull(adviceChain, "adviceChain must not be null");
		this.adviceChain = adviceChain;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.beanName == null) {
			LOGGER.error(() -> "The MessageHandler [" + this.handler + "] will be created without a 'componentName'. " +
					"Consider specifying the 'beanName' property on this ConsumerEndpointFactoryBean.");
		}
		else {
			populateComponentNameIfAny();
		}

		if (!(this.handler instanceof ReactiveMessageHandlerAdapter)) {
			this.handler = adviceChain(this.handler);
		}
		else if (!CollectionUtils.isEmpty(this.adviceChain)) {
			this.handler =
					new ReactiveMessageHandlerAdapter(
							adviceChain(((ReactiveMessageHandlerAdapter) this.handler).getDelegate()));
		}
		if (this.channelResolver == null) {
			this.channelResolver = ChannelResolverUtils.getChannelResolver(this.beanFactory);
		}
		initializeEndpoint();
	}

	private void populateComponentNameIfAny() {
		try {
			if (!this.beanName.startsWith("org.springframework")) {
				MessageHandler targetHandler = this.handler;
				if (AopUtils.isAopProxy(targetHandler)) {
					Object target = ((Advised) targetHandler).getTargetSource().getTarget();
					if (target instanceof MessageHandler) {
						targetHandler = (MessageHandler) target;
					}
				}
				if (targetHandler instanceof IntegrationObjectSupport) {
					((IntegrationObjectSupport) targetHandler).setComponentName(this.beanName);
				}
			}
		}
		catch (Exception ex) {
			LOGGER.debug(() -> "Could not set component name for handler "
					+ this.handler + " for " + this.beanName + " :" + ex.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private <H> H adviceChain(H handler) {
		H theHandler = handler;
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			/*
			 *  ARPMHs advise the handleRequestMessage method internally and already have the advice chain injected.
			 *  So we only advise handlers that are not reply-producing.
			 *  Or if one (or more) of advices is IdempotentReceiverInterceptor.
			 *  If the handler is already advised,
			 *  add the configured advices to its chain, otherwise create a proxy.
			 */
			Class<?> targetClass = AopUtils.getTargetClass(theHandler);
			boolean replyMessageHandler = AbstractReplyProducingMessageHandler.class.isAssignableFrom(targetClass);

			for (Advice advice : this.adviceChain) {
				if (!replyMessageHandler || advice instanceof HandleMessageAdvice) {
					NameMatchMethodPointcutAdvisor handlerAdvice = new NameMatchMethodPointcutAdvisor(advice);
					handlerAdvice.addMethodName("handleMessage");
					if (theHandler instanceof Advised) {
						((Advised) theHandler).addAdvisor(handlerAdvice);
					}
					else {
						ProxyFactory proxyFactory = new ProxyFactory(theHandler);
						proxyFactory.addAdvisor(handlerAdvice);
						theHandler = (H) proxyFactory.getProxy(this.beanClassLoader);
					}
				}
			}
		}
		return theHandler;
	}

	@Override
	public AbstractEndpoint getObject() {
		if (!this.initialized) {
			this.initializeEndpoint();
		}
		return this.endpoint;
	}

	@Override
	public Class<?> getObjectType() {
		if (this.endpoint == null) {
			return AbstractEndpoint.class;
		}
		return this.endpoint.getClass();
	}

	private void initializeEndpoint() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			MessageChannel channel = resolveInputChannel();

			Assert.state(this.reactiveCustomizer == null || this.pollerMetadata == null,
					"The 'pollerMetadata' and 'reactiveCustomizer' are mutually exclusive.");

			if (channel instanceof Publisher ||
					this.handler instanceof ReactiveMessageHandlerAdapter ||
					this.reactiveCustomizer != null) {

				reactiveStreamsConsumer(channel);
			}
			else if (channel instanceof SubscribableChannel) {
				eventDrivenConsumer(channel);
			}
			else if (channel instanceof PollableChannel) {
				pollingConsumer(channel);
			}
			else {
				throw new IllegalArgumentException("Unsupported 'inputChannel' type: '"
						+ channel.getClass().getName() + "'. " +
						"Must be one of 'SubscribableChannel', 'PollableChannel' " +
						"or 'ReactiveStreamsSubscribableChannel'");
			}
			this.endpoint.setBeanName(this.beanName);
			this.endpoint.setBeanFactory(this.beanFactory);
			smartLifecycle();
			this.endpoint.setRole(this.role);
			if (this.taskScheduler != null) {
				this.endpoint.setTaskScheduler(this.taskScheduler);
			}
			this.endpoint.afterPropertiesSet();
			this.initialized = true;
		}
	}

	private MessageChannel resolveInputChannel() {
		MessageChannel channel = null;
		if (StringUtils.hasText(this.inputChannelName)) {
			channel = this.channelResolver.resolveDestination(this.inputChannelName);
		}
		if (this.inputChannel != null) {
			channel = this.inputChannel;
		}
		Assert.state(channel != null, "one of inputChannelName or inputChannel is required");
		return channel;
	}

	private void reactiveStreamsConsumer(MessageChannel channel) {
		ReactiveStreamsConsumer reactiveStreamsConsumer;
		if (this.handler instanceof ReactiveMessageHandlerAdapter) {
			reactiveStreamsConsumer = new ReactiveStreamsConsumer(channel,
					((ReactiveMessageHandlerAdapter) this.handler).getDelegate());
		}
		else {
			reactiveStreamsConsumer = new ReactiveStreamsConsumer(channel, this.handler);
		}

		reactiveStreamsConsumer.setReactiveCustomizer(this.reactiveCustomizer);

		this.endpoint = reactiveStreamsConsumer;
	}

	private void eventDrivenConsumer(MessageChannel channel) {
		Assert.isNull(this.pollerMetadata,
				() -> "A poller should not be specified for endpoint '" + this.beanName
						+ "', since '" + channel + "' is a SubscribableChannel (not pollable).");
		this.endpoint = new EventDrivenConsumer((SubscribableChannel) channel, this.handler);
		if (Boolean.FALSE.equals(this.autoStartup) && channel instanceof FixedSubscriberChannel) {
			LOGGER.warn("'autoStartup=\"false\"' has no effect when using a FixedSubscriberChannel");
		}
	}

	private void pollingConsumer(MessageChannel channel) {
		PollingConsumer pollingConsumer = new PollingConsumer((PollableChannel) channel, this.handler);
		if (this.pollerMetadata == null) {
			this.pollerMetadata = PollerMetadata.getDefaultPollerMetadata(this.beanFactory);
			Assert.notNull(this.pollerMetadata,
					() -> "No poller has been defined for endpoint '" + this.beanName
							+ "', and no default poller is available within the context.");
		}
		pollingConsumer.setTaskExecutor(this.pollerMetadata.getTaskExecutor());
		pollingConsumer.setTrigger(this.pollerMetadata.getTrigger());
		pollingConsumer.setAdviceChain(this.pollerMetadata.getAdviceChain());
		pollingConsumer.setMaxMessagesPerPoll(this.pollerMetadata.getMaxMessagesPerPoll());

		pollingConsumer.setErrorHandler(this.pollerMetadata.getErrorHandler());

		pollingConsumer.setReceiveTimeout(this.pollerMetadata.getReceiveTimeout());
		pollingConsumer.setTransactionSynchronizationFactory(
				this.pollerMetadata.getTransactionSynchronizationFactory());
		pollingConsumer.setBeanClassLoader(this.beanClassLoader);
		pollingConsumer.setBeanFactory(this.beanFactory);
		this.endpoint = pollingConsumer;
	}

	private void smartLifecycle() {
		if (this.autoStartup != null) {
			this.endpoint.setAutoStartup(this.autoStartup);
		}
		int phaseToSet = this.phase;
		if (!this.isPhaseSet) {
			if (this.endpoint instanceof PollingConsumer) {
				phaseToSet = Integer.MAX_VALUE / 2;
			}
			else {
				phaseToSet = Integer.MIN_VALUE;
			}
		}

		this.endpoint.setPhase(phaseToSet);
	}


	/*
	 * SmartLifecycle implementation (delegates to the created endpoint)
	 */

	@Override
	public boolean isAutoStartup() {
		return (this.endpoint == null) || this.endpoint.isAutoStartup();
	}

	@Override
	public int getPhase() {
		return (this.endpoint != null) ? this.endpoint.getPhase() : 0;
	}

	@Override
	public boolean isRunning() {
		return (this.endpoint != null) && this.endpoint.isRunning();
	}

	@Override
	public void start() {
		if (this.endpoint != null) {
			this.endpoint.start();
		}
	}

	@Override
	public void stop() {
		if (this.endpoint != null) {
			this.endpoint.stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.endpoint != null) {
			this.endpoint.stop(callback);
		}
		else {
			callback.run();
		}
	}

	@Override
	public void destroy() {
		if (this.endpoint != null) {
			this.endpoint.destroy();
		}
	}

}
