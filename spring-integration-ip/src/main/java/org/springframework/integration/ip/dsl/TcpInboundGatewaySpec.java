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

import java.util.Collection;
import java.util.Collections;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.scheduling.TaskScheduler;

/**
 * A {@link MessagingGatewaySpec} for {@link TcpInboundGateway}s.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class TcpInboundGatewaySpec extends MessagingGatewaySpec<TcpInboundGatewaySpec, TcpInboundGateway>
		implements ComponentsRegistration {

	private final AbstractConnectionFactory connectionFactory;

	TcpInboundGatewaySpec(AbstractConnectionFactory connectionFactory) {
		super(new TcpInboundGateway());
		this.connectionFactory = connectionFactory;
		this.target.setConnectionFactory(connectionFactory);
	}

	/**
	 * @param isClientMode true to connect in client mode
	 * @return the spec.
	 * @see TcpInboundGateway#setClientMode(boolean)
	 */
	public TcpInboundGatewaySpec clientMode(boolean isClientMode) {
		this.target.setClientMode(isClientMode);
		return _this();
	}

	/**
	 * @param retryInterval the client mode retry interval to set.
	 * @return the spec.
	 * @see TcpInboundGateway#setRetryInterval(long)
	 */
	public TcpInboundGatewaySpec retryInterval(long retryInterval) {
		this.target.setRetryInterval(retryInterval);
		return _this();
	}

	/**
	 * @param taskScheduler the scheduler for connecting in client mode.
	 * @return the spec.
	 * @see TcpInboundGateway#setTaskScheduler(TaskScheduler)
	 */
	public TcpInboundGatewaySpec taskScheduler(TaskScheduler taskScheduler) {
		this.target.setTaskScheduler(taskScheduler);
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.singletonList(this.connectionFactory);
	}

}
