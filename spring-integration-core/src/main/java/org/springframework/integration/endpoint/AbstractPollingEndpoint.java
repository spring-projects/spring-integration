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

package org.springframework.integration.endpoint;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.aopalliance.aop.Advice;
import org.reactivestreams.Subscription;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.aop.ReceiveMessageAdvice;
import org.springframework.integration.channel.ChannelUtils;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.support.MessagingExceptionWrapper;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.IntegrationResourceHolderSynchronization;
import org.springframework.integration.transaction.PassThroughTransactionSynchronizationFactory;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * An {@link AbstractEndpoint} extension for Polling Consumer pattern basics.
 * The standard polling logic is based on a periodic task scheduling according the provided
 * {@link Trigger}.
 * When this endpoint is treated as {@link #isReactive()}, a polling logic is turned into a
 * {@link Flux#generate(java.util.function.Consumer)} and {@link Mono#delay(Duration)} combination based on the
 * {@link SimpleTriggerContext} state.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Andreas Baer
 */
public abstract class AbstractPollingEndpoint extends AbstractEndpoint implements BeanClassLoaderAware {

	/**
	 * A default polling period for {@link PeriodicTrigger}.
	 */
	public static final long DEFAULT_POLLING_PERIOD = 10;

	private final Collection<Advice> appliedAdvices = new HashSet<>();

	private final Object initializationMonitor = new Object();

	private Executor taskExecutor = new SyncTaskExecutor();

	private boolean syncExecutor = true;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Trigger trigger = new PeriodicTrigger(DEFAULT_POLLING_PERIOD);

	private ErrorHandler errorHandler;

	private boolean errorHandlerIsDefault;

	private List<Advice> adviceChain;

	private TransactionSynchronizationFactory transactionSynchronizationFactory;

	private volatile long maxMessagesPerPoll = -1;

	private volatile Callable<Message<?>> pollingTask;

	private volatile Flux<Message<?>> pollingFlux;

	private volatile Subscription subscription;

	private volatile ScheduledFuture<?> runningTask;

	private volatile boolean initialized;

	public AbstractPollingEndpoint() {
		this.setPhase(Integer.MAX_VALUE / 2);
	}

	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = (taskExecutor != null ? taskExecutor : new SyncTaskExecutor());
		this.syncExecutor = this.taskExecutor instanceof SyncTaskExecutor
				|| (this.taskExecutor instanceof ErrorHandlingTaskExecutor
				&& ((ErrorHandlingTaskExecutor) this.taskExecutor).isSyncExecutor());
	}

	protected Executor getTaskExecutor() {
		return this.taskExecutor;
	}

	protected boolean isSyncExecutor() {
		return this.syncExecutor;
	}

	public void setTrigger(Trigger trigger) {
		this.trigger = (trigger != null ? trigger : new PeriodicTrigger(DEFAULT_POLLING_PERIOD));
	}

	public void setAdviceChain(List<Advice> adviceChain) {
		this.adviceChain = adviceChain;
	}

	/**
	 * Configure a cap for messages to poll from the source per scheduling cycle.
* A negative number means retrieve unlimited messages until the {@code MessageSource} returns {@code null}.
	 * Zero means do not poll for any records - it
	 * can be considered as pausing if 'maxMessagesPerPoll' is later changed to a non-zero value.
	 * The polling cycle may exit earlier if the source returns null for the current receive call.
	 * @param maxMessagesPerPoll the number of message to poll per schedule.
	 */
	@ManagedAttribute
	public void setMaxMessagesPerPoll(long maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public long getMaxMessagesPerPoll() {
		return this.maxMessagesPerPoll;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setTransactionSynchronizationFactory(TransactionSynchronizationFactory
			transactionSynchronizationFactory) {
		this.transactionSynchronizationFactory = transactionSynchronizationFactory;
	}

	/**
	 * Return the default error channel if the error handler is explicitly provided and
	 * it is a {@link MessagePublishingErrorHandler}.
	 * @return the channel or null.
	 * @since 4.3
	 */
	public MessageChannel getDefaultErrorChannel() {
		if (!this.errorHandlerIsDefault && this.errorHandler instanceof MessagePublishingErrorHandler) {
			return ((MessagePublishingErrorHandler) this.errorHandler).getDefaultErrorChannel();
		}
		else {
			return null;
		}
	}

	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Return true if this advice should be applied only to the {@link #receiveMessage()} operation
	 * rather than the whole poll.
	 * @param advice The advice.
	 * @return true to only advise the receive operation.
	 */
	protected boolean isReceiveOnlyAdvice(Advice advice) {
		return advice instanceof ReceiveMessageAdvice;
	}

	/**
	 * Add the advice chain to the component that responds to {@link #receiveMessage()} calls.
	 * @param chain the advice chain {@code Collection}.
	 */
	protected void applyReceiveOnlyAdviceChain(Collection<Advice> chain) {
		if (!CollectionUtils.isEmpty(chain)) {
			Object source = getReceiveMessageSource();
			if (source != null) {
				if (AopUtils.isAopProxy(source)) {
					Advised advised = (Advised) source;
					this.appliedAdvices.forEach(advised::removeAdvice);
					chain.forEach(advice -> advised.addAdvisor(adviceToReceiveAdvisor(advice)));
				}
				else {
					ProxyFactory proxyFactory = new ProxyFactory(source);
					chain.forEach(advice -> proxyFactory.addAdvisor(adviceToReceiveAdvisor(advice)));
					source = proxyFactory.getProxy(getBeanClassLoader());
				}
				this.appliedAdvices.clear();
				this.appliedAdvices.addAll(chain);
				if (!(isSyncExecutor())) {
					logger.warn(() -> getComponentName() + ": A task executor is supplied and " + chain.size()
							+ "ReceiveMessageAdvice(s) is/are provided. If an advice mutates the source, such "
							+ "mutations are not thread safe and could cause unexpected results, especially with "
							+ "high frequency pollers. Consider using a downstream ExecutorChannel instead of "
							+ "adding an executor to the poller");
				}
				setReceiveMessageSource(source);
			}
		}
	}

	private NameMatchMethodPointcutAdvisor adviceToReceiveAdvisor(Advice advice) {
		NameMatchMethodPointcutAdvisor sourceAdvisor = new NameMatchMethodPointcutAdvisor(advice);
		sourceAdvisor.addMethodName("receive");
		return sourceAdvisor;
	}

	protected boolean isReactive() {
		return false;
	}

	protected Flux<Message<?>> getPollingFlux() {
		return this.pollingFlux;
	}

	protected Object getReceiveMessageSource() {
		return null;
	}

	protected void setReceiveMessageSource(Object source) {

	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.trigger, "Trigger is required");
			if (this.taskExecutor != null && !(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
				if (this.errorHandler == null) {
					this.errorHandler = ChannelUtils.getErrorHandler(getBeanFactory());
					this.errorHandlerIsDefault = true;
				}
				this.taskExecutor = new ErrorHandlingTaskExecutor(this.taskExecutor, this.errorHandler);
			}
			if (this.transactionSynchronizationFactory == null && this.adviceChain != null &&
					this.adviceChain.stream().anyMatch(TransactionInterceptor.class::isInstance)) {
				this.transactionSynchronizationFactory = new PassThroughTransactionSynchronizationFactory();
			}
			this.initialized = true;
		}
		try {
			super.onInit();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Cannot initialize: " + this, ex);
		}
	}

	// LifecycleSupport implementation

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		if (!this.initialized) {
			onInit();
		}

		this.pollingTask = createPollingTask();

		if (isReactive()) {
			this.pollingFlux = createFluxGenerator();
		}
		else {
			TaskScheduler taskScheduler = getTaskScheduler();
			Assert.state(taskScheduler != null, "unable to start polling, no taskScheduler available");
			this.runningTask = taskScheduler.schedule(createPoller(), this.trigger);
		}
	}

	@SuppressWarnings("unchecked")
	private Callable<Message<?>> createPollingTask() {
		List<Advice> receiveOnlyAdviceChain = null;
		if (!CollectionUtils.isEmpty(this.adviceChain)) {
			receiveOnlyAdviceChain = this.adviceChain.stream()
					.filter(this::isReceiveOnlyAdvice)
					.collect(Collectors.toList());
		}

		Callable<Message<?>> task = this::doPoll;

		List<Advice> advices = this.adviceChain;
		if (!CollectionUtils.isEmpty(advices)) {
			ProxyFactory proxyFactory = new ProxyFactory(task);
			if (!CollectionUtils.isEmpty(advices)) {
				advices.stream()
						.filter(advice -> !isReceiveOnlyAdvice(advice))
						.forEach(proxyFactory::addAdvice);
			}
			task = (Callable<Message<?>>) proxyFactory.getProxy(this.beanClassLoader);
		}
		if (!CollectionUtils.isEmpty(receiveOnlyAdviceChain)) {
			applyReceiveOnlyAdviceChain(receiveOnlyAdviceChain);
		}

		return task;
	}

	private Runnable createPoller() {
		return () ->
				this.taskExecutor.execute(() -> {
					int count = 0;
					while (this.initialized && (this.maxMessagesPerPoll <= 0 || count < this.maxMessagesPerPoll)) {
						if (this.maxMessagesPerPoll == 0) {
							logger.info("Polling disabled while 'maxMessagesPerPoll == 0'");
							break;
						}
						if (pollForMessage() == null) {
							break;
						}
						count++;
					}
				});
	}

	private Flux<Message<?>> createFluxGenerator() {
		SimpleTriggerContext triggerContext = new SimpleTriggerContext();

		return Flux
				.<Duration>generate(sink -> {
					Date date = this.trigger.nextExecutionTime(triggerContext);
					if (date != null) {
						triggerContext.update(date, null, null);
						long millis = date.getTime() - System.currentTimeMillis();
						sink.next(Duration.ofMillis(millis));
					}
					else {
						sink.complete();
					}
				})
				.concatMap(duration ->
						Mono.delay(duration)
								.doOnNext(l ->
										triggerContext.update(triggerContext.lastScheduledExecutionTime(),
												new Date(), null))
								.flatMapMany(l ->
										Flux
												.defer(() -> {
													if (this.maxMessagesPerPoll == 0) {
														logger.info("Polling disabled while 'maxMessagesPerPoll == 0'");
														return Mono.empty();
													}
													else {
														return Flux
																.<Message<?>>generate(fluxSink -> {
																	Message<?> message = pollForMessage();
																	if (message != null) {
																		fluxSink.next(message);
																	}
																	else {
																		fluxSink.complete();
																	}
																})
																.limitRequest(
																		this.maxMessagesPerPoll < 0
																				? Long.MAX_VALUE
																				: this.maxMessagesPerPoll);
													}
												})
												.subscribeOn(Schedulers.fromExecutor(this.taskExecutor))
												.doOnComplete(() ->
														triggerContext
																.update(triggerContext.lastScheduledExecutionTime(),
																		triggerContext.lastActualExecutionTime(),
																		new Date())
												)), 0)
				.repeat(this::isActive)
				.doOnSubscribe(subs -> this.subscription = subs);
	}

	private Message<?> pollForMessage() {
		try {
			return this.pollingTask.call();
		}
		catch (Exception e) {
			if (e instanceof MessagingException) { // NOSONAR
				throw (MessagingException) e;
			}
			else {
				Message<?> failedMessage = null;
				if (this.transactionSynchronizationFactory != null) {
					Object resource = TransactionSynchronizationManager.getResource(getResourceToBind());
					if (resource instanceof IntegrationResourceHolder) {
						failedMessage = ((IntegrationResourceHolder) resource).getMessage();
					}
				}
				throw new MessagingException(failedMessage, e); // NOSONAR (null failedMessage)
			}
		}
		finally {
			if (this.transactionSynchronizationFactory != null) {
				Object resource = getResourceToBind();
				if (TransactionSynchronizationManager.hasResource(resource)) {
					TransactionSynchronizationManager.unbindResource(resource);
				}
			}
		}
	}

	private Message<?> doPoll() {
		IntegrationResourceHolder holder = bindResourceHolderIfNecessary(getResourceKey(), getResourceToBind());
		Message<?> message = null;
		try {
			message = receiveMessage();
		}
		catch (Exception ex) {
			if (Thread.interrupted()) {
				logger.debug(() -> "Poll interrupted - during stop()? : " + ex.getMessage());
				return null;
			}
			else {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
		}

		if (message == null) {
			this.logger.debug("Received no Message during the poll, returning 'false'");
			return null;
		}
		else {
			messageReceived(holder, message);
		}

		return message;
	}

	private void messageReceived(IntegrationResourceHolder holder, Message<?> message) {
		this.logger.debug(() -> "Poll resulted in Message: " + message);
		if (holder != null) {
			holder.setMessage(message);
		}

		if (!isReactive()) {
			try {
				handleMessage(message);
			}
			catch (MessagingException ex) {
				throw new MessagingExceptionWrapper(message, ex);
			}
			catch (Exception ex) {
				throw new MessagingException(message, ex);
			}
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		if (this.runningTask != null) {
			this.runningTask.cancel(true);
		}
		this.runningTask = null;

		if (this.subscription != null) {
			this.subscription.cancel();
		}
	}

	/**
	 * Obtain the next message (if one is available). MAY return null
	 * if no message is immediately available.
	 * @return The message or null.
	 */
	protected abstract Message<?> receiveMessage();

	/**
	 * Handle a message.
	 * @param message The message.
	 */
	protected abstract void handleMessage(Message<?> message);

	/**
	 * Return a resource (MessageSource etc) to bind when using transaction
	 * synchronization.
	 * @return The resource, or null if transaction synchronization is not required.
	 */
	protected Object getResourceToBind() {
		return null;
	}

	/**
	 * Return the key under which the resource will be made available as an
	 * attribute on the {@link IntegrationResourceHolder}. The default
	 * {@link org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor}
	 * makes this attribute available as a variable in SpEL expressions.
	 * @return The key, or null (default) if the resource shouldn't be
	 * made available as a attribute.
	 */
	protected String getResourceKey() {
		return null;
	}

	private IntegrationResourceHolder bindResourceHolderIfNecessary(String key, Object resource) {
		if (this.transactionSynchronizationFactory != null && resource != null &&
				TransactionSynchronizationManager.isActualTransactionActive()) {

			TransactionSynchronization synchronization = this.transactionSynchronizationFactory.create(resource);
			if (synchronization != null) {
				TransactionSynchronizationManager.registerSynchronization(synchronization);

				if (synchronization instanceof IntegrationResourceHolderSynchronization) {
					IntegrationResourceHolderSynchronization integrationSynchronization =
							((IntegrationResourceHolderSynchronization) synchronization);
					integrationSynchronization.setShouldUnbindAtCompletion(false);

					if (!TransactionSynchronizationManager.hasResource(resource)) {
						TransactionSynchronizationManager.bindResource(resource,
								integrationSynchronization.getResourceHolder());
					}
				}
			}

			Object resourceHolder = TransactionSynchronizationManager.getResource(resource);
			if (resourceHolder instanceof IntegrationResourceHolder) {
				IntegrationResourceHolder integrationResourceHolder = (IntegrationResourceHolder) resourceHolder;
				if (key != null) {
					integrationResourceHolder.addAttribute(key, resource);
				}
				return integrationResourceHolder;
			}
		}

		return null;
	}

}
