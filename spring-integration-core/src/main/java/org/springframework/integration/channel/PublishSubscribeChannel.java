/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.channel;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * A channel that sends Messages to each of its subscribers.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PublishSubscribeChannel extends AbstractExecutorChannel implements BroadcastCapableChannel {

	private ErrorHandler errorHandler;

	private boolean ignoreFailures;

	private boolean applySequence;

	private int minSubscribers;

	/**
	 * Create a PublishSubscribeChannel that will invoke the handlers in the
	 * message sender's thread.
	 */
	public PublishSubscribeChannel() {
		this(null);
	}

	/**
	 * Create a PublishSubscribeChannel that will use an {@link Executor}
	 * to invoke the handlers. If this is null, each invocation will occur in
	 * the message sender's thread.
	 * @param executor The executor.
	 */
	public PublishSubscribeChannel(@Nullable Executor executor) {
		super(executor);
		this.dispatcher = new BroadcastingDispatcher(executor);
	}


	@Override
	public String getComponentType() {
		return "publish-subscribe-channel";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.publish_subscribe_channel;
	}

	/**
	 * Provide an {@link ErrorHandler} strategy for handling Exceptions that
	 * occur downstream from this channel. This will <i>only</i> be applied if
	 * an Executor has been configured to dispatch the Messages for this
	 * channel. Otherwise, Exceptions will be thrown directly within the
	 * sending Thread. If no ErrorHandler is provided, and this channel does
	 * delegate its dispatching to an Executor, the default strategy is
	 * a {@link MessagePublishingErrorHandler} that sends error messages to
	 * the failed request Message's error channel header if available or to
	 * the default 'errorChannel' otherwise.
	 * @param errorHandler The error handler.
	 * @see #PublishSubscribeChannel(Executor)
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Specify whether failures for one or more of the handlers should be
	 * ignored. By default this is <code>false</code> meaning that an Exception
	 * will be thrown whenever a handler fails. To override this and suppress
	 * Exceptions, set the value to <code>true</code>.
	 * @param ignoreFailures true if failures should be ignored.
	 */
	public void setIgnoreFailures(boolean ignoreFailures) {
		this.ignoreFailures = ignoreFailures;
		getDispatcher().setIgnoreFailures(ignoreFailures);
	}

	/**
	 * Specify whether to apply the sequence number and size headers to the
	 * messages prior to invoking the subscribed handlers. By default, this
	 * value is <code>false</code> meaning that sequence headers will
	 * <em>not</em> be applied. If planning to use an Aggregator downstream
	 * with the default correlation and completion strategies, you should set
	 * this flag to <code>true</code>.
	 * @param applySequence true if the sequence information should be applied.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
		getDispatcher().setApplySequence(applySequence);
	}

	/**
	 * If at least this number of subscribers receive the message,
	 * {@link #send(org.springframework.messaging.Message)}
	 * will return true. Default: 0.
	 * @param minSubscribers The minimum number of subscribers.
	 */
	public void setMinSubscribers(int minSubscribers) {
		this.minSubscribers = minSubscribers;
		getDispatcher().setMinSubscribers(minSubscribers);
	}

	/**
	 * Callback method for initialization.
	 */
	@Override
	public final void onInit() {
		super.onInit();
		BeanFactory beanFactory = getBeanFactory();
		BroadcastingDispatcher dispatcherToUse = getDispatcher();
		if (this.executor != null) {
			Assert.state(dispatcherToUse.getHandlerCount() == 0,
					"When providing an Executor, you cannot subscribe() until the channel "
							+ "bean is fully initialized by the framework. Do not subscribe in a @Bean definition");
			if (!(this.executor instanceof ErrorHandlingTaskExecutor)) {
				if (this.errorHandler == null) {
					this.errorHandler = ChannelUtils.getErrorHandler(beanFactory);
				}
				this.executor = new ErrorHandlingTaskExecutor(this.executor, this.errorHandler);
			}
			dispatcherToUse = new BroadcastingDispatcher(this.executor);
			dispatcherToUse.setIgnoreFailures(this.ignoreFailures);
			dispatcherToUse.setApplySequence(this.applySequence);
			dispatcherToUse.setMinSubscribers(this.minSubscribers);
			this.dispatcher = dispatcherToUse;
		}
		else if (this.errorHandler != null && this.logger.isWarnEnabled()) {
			this.logger.warn("The 'errorHandler' is ignored for the '" + getComponentName() +
					"' (an 'executor' is not provided) and exceptions will be thrown " +
					"directly within the sending Thread");
		}

		if (this.maxSubscribers == null) {
			Integer maxSubscribers =
					getIntegrationProperty(IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS, Integer.class);
			setMaxSubscribers(maxSubscribers);
		}
		dispatcherToUse.setBeanFactory(beanFactory);

		dispatcherToUse.setMessageHandlingTaskDecorator(task -> {
			if (PublishSubscribeChannel.this.executorInterceptorsSize > 0) {
				return new MessageHandlingTask(task);
			}
			else {
				return task;
			}
		});
	}

	@Override
	protected BroadcastingDispatcher getDispatcher() {
		return (BroadcastingDispatcher) this.dispatcher;
	}

}
