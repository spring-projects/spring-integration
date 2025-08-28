/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.gateway;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * The {@link AbstractReplyProducingMessageHandler} implementation for mid-flow Gateway.
 *
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 5.0
 */
public class GatewayMessageHandler extends AbstractReplyProducingMessageHandler implements ManageableLifecycle {

	private final Lock lock = new ReentrantLock();

	@SuppressWarnings("NullAway.Init")
	private volatile GatewayProxyFactoryBean<?> gatewayProxyFactoryBean;

	@SuppressWarnings("NullAway.Init")
	private volatile Object exchanger;

	private volatile boolean running;

	private @Nullable MessageChannel requestChannel;

	private @Nullable String requestChannelName;

	private @Nullable MessageChannel replyChannel;

	private @Nullable String replyChannelName;

	private @Nullable MessageChannel errorChannel;

	private @Nullable String errorChannelName;

	private @Nullable Long requestTimeout;

	private @Nullable Long replyTimeout;

	private boolean errorOnTimeout;

	private @Nullable Executor executor = new SimpleAsyncTaskExecutor();

	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	public void setRequestChannelName(String requestChannel) {
		this.requestChannelName = requestChannel;
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	public void setReplyChannelName(String replyChannel) {
		this.replyChannelName = replyChannel;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void setErrorChannelName(String errorChannel) {
		this.errorChannelName = errorChannel;
	}

	public void setRequestTimeout(Long requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public void setReplyTimeout(Long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	public void setErrorOnTimeout(boolean errorOnTimeout) {
		this.errorOnTimeout = errorOnTimeout;
	}

	/**
	 * Set the executor for use when the gateway method returns
	 * {@link Future} or {@link CompletableFuture}.
	 * Set it to null to disable the async processing, and any
	 * {@link Future} return types must be returned by the downstream flow.
	 * @param executor The executor.
	 */
	public void setAsyncExecutor(@Nullable Executor executor) {
		this.executor = executor;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		if (this.exchanger == null) {
			this.lock.lock();
			try {
				if (this.exchanger == null) {
					initialize();
				}
			}
			finally {
				this.lock.unlock();
			}
		}
		return isAsync()
				? ((AsyncRequestReplyExchanger) this.exchanger).exchange(requestMessage)
				: ((RequestReplyExchanger) this.exchanger).exchange(requestMessage);
	}

	private void initialize() {
		GatewayProxyFactoryBean<?> newGatewayProxyFactoryBean;
		if (isAsync()) {
			newGatewayProxyFactoryBean = new GatewayProxyFactoryBean<>(AsyncRequestReplyExchanger.class);
		}
		else {
			newGatewayProxyFactoryBean = new GatewayProxyFactoryBean<>(RequestReplyExchanger.class);
		}

		JavaUtils.INSTANCE
				.acceptIfNotNull(this.requestChannel, newGatewayProxyFactoryBean::setDefaultRequestChannel)
				.acceptIfHasText(this.requestChannelName, newGatewayProxyFactoryBean::setDefaultRequestChannelName)
				.acceptIfNotNull(this.replyChannel, newGatewayProxyFactoryBean::setDefaultReplyChannel)
				.acceptIfHasText(this.replyChannelName, newGatewayProxyFactoryBean::setDefaultReplyChannelName)
				.acceptIfNotNull(this.errorChannel, newGatewayProxyFactoryBean::setErrorChannel)
				.acceptIfHasText(this.errorChannelName, newGatewayProxyFactoryBean::setErrorChannelName)
				.acceptIfNotNull(this.requestTimeout, newGatewayProxyFactoryBean::setDefaultRequestTimeout)
				.acceptIfNotNull(this.replyTimeout, newGatewayProxyFactoryBean::setDefaultReplyTimeout);

		newGatewayProxyFactoryBean.setAsyncExecutor(this.executor);
		newGatewayProxyFactoryBean.setErrorOnTimeout(this.errorOnTimeout);
		newGatewayProxyFactoryBean.setBeanName(getComponentName() + "#gpfb");
		newGatewayProxyFactoryBean.setBeanFactory(getBeanFactory());
		newGatewayProxyFactoryBean.setApplicationContext(getApplicationContext());
		newGatewayProxyFactoryBean.setBeanClassLoader(getBeanClassLoader());
		newGatewayProxyFactoryBean.afterPropertiesSet();

		try {
			this.exchanger = newGatewayProxyFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BeanCreationException("Can't instantiate the GatewayProxyFactoryBean: " + this, e);
		}
		if (this.running) {
			// We must stop gatewayProxyFactoryBean because after the normal start its "gatewayMap" is still empty
			newGatewayProxyFactoryBean.stop();
			newGatewayProxyFactoryBean.start();
		}

		this.gatewayProxyFactoryBean = newGatewayProxyFactoryBean;
	}

	@Override
	public void start() {
		if (this.exchanger == null) {
			this.lock.lock();
			try {
				if (this.exchanger == null) {
					initialize();
				}
			}
			finally {
				this.lock.unlock();
			}
		}
		this.gatewayProxyFactoryBean.start();
		this.running = true;
	}

	@Override
	public void stop() {
		this.gatewayProxyFactoryBean.stop();
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
