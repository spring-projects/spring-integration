/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link MessageProducerSpec} for {@link TcpReceivingChannelAdapter}s.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class TcpInboundChannelAdapterSpec<S extends TcpInboundChannelAdapterSpec<S>>
	extends MessageProducerSpec<S, TcpReceivingChannelAdapter> {

	TcpInboundChannelAdapterSpec(AbstractConnectionFactory connectionFactory) {
		super(new TcpReceivingChannelAdapter());
		this.target.setConnectionFactory(connectionFactory);
	}

	/**
	 * @param isClientMode true to connect in client mode
	 * @return the spec.
	 * @see TcpReceivingChannelAdapter#setClientMode(boolean)
	 */
	public S clientMode(boolean isClientMode) {
		this.target.setClientMode(isClientMode);
		return _this();
	}

	/**
	 * @param retryInterval the client mode retry interval to set.
	 * @return the spec.
	 * @see TcpReceivingChannelAdapter#setRetryInterval(long)
	 */
	public S retryInterval(long retryInterval) {
		this.target.setRetryInterval(retryInterval);
		return _this();
	}

	/**
	 * @param taskScheduler the scheduler for connecting in client mode.
	 * @return the spec.
	 * @see TcpReceivingChannelAdapter#setTaskScheduler(TaskScheduler)
	 */
	public S taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return _this();
	}

}
