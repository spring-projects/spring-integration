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

package org.springframework.integration.channel;

import java.util.concurrent.Executor;

import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.util.ErrorHandler;

/**
 * A channel that sends Messages to each of its subscribers.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class PublishSubscribeChannel extends AbstractSubscribableChannel {

	private volatile BroadcastingDispatcher dispatcher;

	private volatile Executor executor;

	private volatile ErrorHandler errorHandler;

	private volatile boolean ignoreFailures;

	private volatile boolean applySequence;

	private volatile int minSubscribers;

	private volatile Integer maxSubscribers;

	@Override
	public String getComponentType(){
		return "publish-subscribe-channel";
	}
	/**
	 * Create a PublishSubscribeChannel that will use an {@link Executor}
	 * to invoke the handlers. If this is null, each invocation will occur in
	 * the message sender's thread.
	 *
	 * @param executor The executor.
	 */
	public PublishSubscribeChannel(Executor executor) {
		this.executor = executor;
		this.dispatcher = new BroadcastingDispatcher(executor);
	}

	/**
	 * Create a PublishSubscribeChannel that will invoke the handlers in the
	 * message sender's thread.
	 */
	public PublishSubscribeChannel() {
		this(null);
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
	 *
	 * @param errorHandler The error handler.
	 *
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
	 *
	 * @param ignoreFailures true if failures should be ignored.
	 */
	public void setIgnoreFailures(boolean ignoreFailures) {
		this.ignoreFailures = ignoreFailures;
		this.getDispatcher().setIgnoreFailures(ignoreFailures);
	}

	/**
	 * Specify whether to apply the sequence number and size headers to the
	 * messages prior to invoking the subscribed handlers. By default, this
	 * value is <code>false</code> meaning that sequence headers will
	 * <em>not</em> be applied. If planning to use an Aggregator downstream
	 * with the default correlation and completion strategies, you should set
	 * this flag to <code>true</code>.
	 *
	 * @param applySequence true if the sequence information should be applied.
	 */
	public void setApplySequence(boolean applySequence) {
		this.applySequence = applySequence;
		this.getDispatcher().setApplySequence(applySequence);
	}

	/**
	 * Specify the maximum number of subscribers supported by the
	 * channel's dispatcher.
	 *
	 * @param maxSubscribers The maximum number of subscribers allowed.
	 */
	public void setMaxSubscribers(int maxSubscribers) {
		this.maxSubscribers = maxSubscribers;
		this.getDispatcher().setMaxSubscribers(maxSubscribers);
	}

	/**
	 * If at least this number of subscribers receive the message,
	 * {@link #send(org.springframework.messaging.Message)}
	 * will return true. Default: 0.
	 *
	 * @param minSubscribers The minimum number of subscribers.
	 */
	public void setMinSubscribers(int minSubscribers) {
		this.minSubscribers = minSubscribers;
		this.getDispatcher().setMinSubscribers(minSubscribers);
	}

	/**
	 * Callback method for initialization.
	 */
	@Override
	public final void onInit() {
		if (this.executor != null) {
			if (!(this.executor instanceof ErrorHandlingTaskExecutor)) {
				if (this.errorHandler == null) {
					this.errorHandler = new MessagePublishingErrorHandler(
							new BeanFactoryChannelResolver(this.getBeanFactory()));
				}
				this.executor = new ErrorHandlingTaskExecutor(this.executor, this.errorHandler);
			}
			this.dispatcher = new BroadcastingDispatcher(this.executor);
			this.dispatcher.setIgnoreFailures(this.ignoreFailures);
			this.dispatcher.setApplySequence(this.applySequence);
			this.dispatcher.setMinSubscribers(this.minSubscribers);
		}
		if (this.maxSubscribers == null) {
			Integer maxSubscribers = this.getIntegrationProperty(IntegrationProperties.CHANNELS_MAX_BROADCAST_SUBSCRIBERS, Integer.class);
			this.setMaxSubscribers(maxSubscribers);
		}
		this.dispatcher.setBeanFactory(this.getBeanFactory());
	}

	@Override
	protected BroadcastingDispatcher getDispatcher() {
		return this.dispatcher;
	}

}
