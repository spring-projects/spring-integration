/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.ip.dsl;

import java.util.Collections;
import java.util.Map;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link MessageHandlerSpec} for {@link TcpSendingMessageHandler}s.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 *
 */
public class TcpOutboundChannelAdapterSpec
		extends MessageHandlerSpec<TcpOutboundChannelAdapterSpec, TcpSendingMessageHandler>
		implements ComponentsRegistration {

	protected final AbstractConnectionFactory connectionFactory; // NOSONAR - final

	/**
	 * Construct an instance using an existing spring-managed connection factory.
	 * @param connectionFactoryBean the spring-managed bean.
	 */
	protected TcpOutboundChannelAdapterSpec(AbstractConnectionFactory connectionFactoryBean) {
		this.target = new TcpSendingMessageHandler();
		this.connectionFactory = null;
		this.target.setConnectionFactory(connectionFactoryBean);
	}

	/**
	 * Construct an instance using the provided connection factory spec.
	 * @param connectionFactorySpec the spec.
	 */
	protected TcpOutboundChannelAdapterSpec(AbstractConnectionFactorySpec<?, ?> connectionFactorySpec) {
		this.target = new TcpSendingMessageHandler();
		this.connectionFactory = connectionFactorySpec.getObject();
		this.target.setConnectionFactory(this.connectionFactory);
	}

	/**
	 * @param isClientMode true to connect in client mode
	 * @return the spec.
	 * @see TcpSendingMessageHandler#setClientMode(boolean)
	 */
	public TcpOutboundChannelAdapterSpec clientMode(boolean isClientMode) {
		this.target.setClientMode(isClientMode);
		return _this();
	}

	/**
	 * @param retryInterval the client mode retry interval to set.
	 * @return the spec.
	 * @see TcpSendingMessageHandler#setRetryInterval(long)
	 */
	public TcpOutboundChannelAdapterSpec retryInterval(long retryInterval) {
		this.target.setRetryInterval(retryInterval);
		return _this();
	}

	/**
	 * @param taskScheduler the scheduler for connecting in client mode.
	 * @return the spec.
	 * @see TcpSendingMessageHandler#setTaskScheduler(TaskScheduler)
	 */
	public TcpOutboundChannelAdapterSpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return this.connectionFactory != null
				? Collections.singletonMap(this.connectionFactory, this.connectionFactory.getComponentName())
				: Collections.emptyMap();
	}

}
