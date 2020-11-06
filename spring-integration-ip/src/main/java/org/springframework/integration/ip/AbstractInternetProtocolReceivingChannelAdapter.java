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

package org.springframework.integration.ip;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * Base class for inbound TCP/UDP Channel Adapters.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractInternetProtocolReceivingChannelAdapter
		extends MessageProducerSupport
		implements ApplicationEventPublisherAware, SchedulingAwareRunnable, CommonSocketOptions {

	private final int port;

	private ApplicationEventPublisher applicationEventPublisher;

	private int soTimeout = 0;

	private int soReceiveBufferSize = -1;

	private int receiveBufferSize = 2048;

	private String localAddress;

	private Executor taskExecutor;

	private boolean taskExecutorSet;

	private int poolSize = 5;

	private volatile boolean listening;


	public AbstractInternetProtocolReceivingChannelAdapter(int port) {
		this.port = port;
	}

	/**
	 *
	 * @return The port on which this receiver is listening.
	 */
	public int getPort() {
		return this.port;
	}

	@Override
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @return the soTimeout
	 */
	public int getSoTimeout() {
		return this.soTimeout;
	}

	@Override
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @return the soReceiveBufferSize
	 */
	public int getSoReceiveBufferSize() {
		return this.soReceiveBufferSize;
	}

	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	/**
	 * @return the receiveBufferSize
	 */
	public int getReceiveBufferSize() {
		return this.receiveBufferSize;
	}

	public boolean isListening() {
		return this.listening;
	}

	/**
	 * @param listening the listening to set
	 */
	public void setListening(boolean listening) {
		this.listening = listening;
	}

	public String getLocalAddress() {
		return this.localAddress;
	}

	@Override
	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public void setTaskExecutor(Executor taskExecutor) {
		Assert.notNull(taskExecutor, "'taskExecutor' cannot be null");
		this.taskExecutor = taskExecutor;
		this.taskExecutorSet = true;
	}

	/**
	 * @return the taskExecutor
	 */
	public Executor getTaskExecutor() {
		return this.taskExecutor;
	}

	protected ApplicationEventPublisher getApplicationEventPublisher() {
		return this.applicationEventPublisher;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Protected by lifecycleLock
	 */
	@Override
	protected void doStart() {
		String beanName = getComponentName();
		checkTaskExecutor((beanName == null ? "" : beanName + "-") + getComponentType());
		this.taskExecutor.execute(this);
	}

	/**
	 * Creates a default task executor if none was supplied.
	 *
	 * @param threadName The thread name.
	 */
	protected void checkTaskExecutor(final String threadName) {
		if (isActive() && this.taskExecutor == null) {
			this.taskExecutor =
					Executors.newFixedThreadPool(this.poolSize,
							(runner) -> {
						Thread thread = new Thread(runner);
						thread.setName(threadName);
						thread.setDaemon(true);
						return thread;
					});
		}
	}

	@Override
	protected void doStop() {
		if (!this.taskExecutorSet && this.taskExecutor != null) {
			((ExecutorService) this.taskExecutor).shutdown();
			this.taskExecutor = null;
		}
	}

}
