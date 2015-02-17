/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.ip.event.IpIntegrationEvent;
import org.springframework.util.Assert;

/**
 * ApplicationEvent representing exceptions on a TCP server socket/channel.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
@SuppressWarnings("serial")
public class TcpConnectionServerExceptionEvent extends IpIntegrationEvent {

	public TcpConnectionServerExceptionEvent(AbstractServerConnectionFactory connectionFactory, Throwable cause) {
		super(connectionFactory, cause);
		Assert.notNull(cause, "'cause' cannot be null");
		Assert.notNull(connectionFactory, "'connectionFactory' cannot be null");
	}

	/**
	 * The connection factory that experienced the exception; examine it to determine the port etc.
	 * @return the connection factory.
	 */
	public AbstractServerConnectionFactory getConnectionFactory() {
		return (AbstractServerConnectionFactory) getSource();
	}

	@Override
	public String toString() {
		return super.toString() +
				", [factory=" + this.getConnectionFactory().getComponentType() +
				":" + this.getConnectionFactory().getComponentName() +
				", port=" + this.getConnectionFactory().getPort() + "]";
	}

}
