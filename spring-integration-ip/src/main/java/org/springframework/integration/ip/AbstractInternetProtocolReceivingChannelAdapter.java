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

package org.springframework.integration.ip;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.util.Assert;

/**
 * Base class for inbound TCP/UDP Channel Adapters.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public abstract class AbstractInternetProtocolReceivingChannelAdapter
		extends MessageProducerSupport implements Runnable, CommonSocketOptions {

	private final int port;

	private volatile int soTimeout = 0;

	private volatile int soReceiveBufferSize = -1;

	private volatile int receiveBufferSize = 2048;

	private volatile boolean active;

	private volatile boolean listening;

	private volatile String localAddress;

	private volatile Executor taskExecutor;

	private volatile boolean taskExecutorSet;

	private volatile int poolSize = 5;


	public AbstractInternetProtocolReceivingChannelAdapter(int port) {
		this.port = port;
	}

	/**
	 *
	 * @return The port on which this receiver is listening.
	 */
	public int getPort() {
		return port;
	}

	@Override
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/**
	 * @return the soTimeout
	 */
	public int getSoTimeout() {
		return soTimeout;
	}

	@Override
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/**
	 * @return the soReceiveBufferSize
	 */
	public int getSoReceiveBufferSize() {
		return soReceiveBufferSize;
	}

	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	/**
	 * @return the receiveBufferSize
	 */
	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	/**
	 * Protected by lifecycleLock
	 */
	@Override
	protected void doStart() {
		if (!this.active) {
			this.active = true;
			String beanName = this.getComponentName();
			checkTaskExecutor((beanName == null ? "" : beanName + "-") + this.getComponentType());
			this.taskExecutor.execute(this);
		}
	}

	/**
	 * Creates a default task executor if none was supplied.
	 *
	 * @param threadName The thread name.
	 */
	protected void checkTaskExecutor(final String threadName) {
		if (this.active && this.taskExecutor == null) {
			Executor executor = Executors.newFixedThreadPool(this.poolSize, new ThreadFactory() {
				@Override
				public Thread newThread(Runnable runner) {
					Thread thread = new Thread(runner);
					thread.setName(threadName);
					thread.setDaemon(true);
					return thread;
				}
			});
			this.taskExecutor = executor;
		}
	}

	@Override
	protected void doStop() {
		this.active = false;
		if (!this.taskExecutorSet && this.taskExecutor != null) {
			((ExecutorService) this.taskExecutor).shutdown();
			this.taskExecutor = null;
		}
	}

	public boolean isListening() {
		return listening;
	}

	/**
	 * @param listening the listening to set
	 */
	public void setListening(boolean listening) {
		this.listening = listening;
	}

	public String getLocalAddress() {
		return localAddress;
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
		return taskExecutor;
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}

}
