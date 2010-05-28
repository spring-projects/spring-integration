/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Date;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.scheduling.TaskScheduler;
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

	protected final int port;

	protected volatile int soTimeout = 0;

	protected volatile int soReceiveBufferSize = -1;

	protected volatile int receiveBufferSize = 2048;

	protected volatile boolean active;

	protected volatile boolean listening;


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

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.SocketOptions#setSoTimeout(int)
	 */
	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.SocketOptions#setSoReceiveBufferSize(int)
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		this.soReceiveBufferSize = soReceiveBufferSize;
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.ip.CommonSocketOptions#setSoSendBufferSize(int)
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
	}

	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	@Override
	protected void doStart() {
		TaskScheduler taskScheduler = this.getTaskScheduler();
		Assert.state(taskScheduler != null, "taskScheduler is required");
		this.active = true;
		taskScheduler.schedule(this, new Date());
	}

	/* (non-Javadoc)
	 * @see org.springframework.integration.endpoint.AbstractEndpoint#doStop()
	 */
	@Override
	protected void doStop() {
		this.active = false;
	}

	public boolean isListening() {
		return listening;
	}

}
