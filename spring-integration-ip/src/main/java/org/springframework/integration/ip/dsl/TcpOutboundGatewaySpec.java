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

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;

/**
 * A {@link MessageHandlerSpec} for {@link TcpOutboundGateway}s.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class TcpOutboundGatewaySpec extends MessageHandlerSpec<TcpOutboundGatewaySpec, TcpOutboundGateway> {

	public TcpOutboundGatewaySpec(AbstractClientConnectionFactory connectionFactory) {
		this.target = new TcpOutboundGateway();
		this.target.setConnectionFactory(connectionFactory);
	}

	public TcpOutboundGatewaySpec remoteTimeout(long remoteTimeout) {
		this.target.setRemoteTimeout(remoteTimeout);
		return _this();
	}

}
