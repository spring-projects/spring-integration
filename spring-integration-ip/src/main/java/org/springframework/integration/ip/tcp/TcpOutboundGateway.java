/*
 * Copyright 2001-present the original author or authors.
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

package org.springframework.integration.ip.tcp;

import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;

/**
 * TCP outbound gateway that uses a client connection factory. If the factory is configured
 * for single-use connections, each request is sent on a new connection; if the factory does not use
 * single use connections, each request is blocked until the previous response is received
 * (or times out). Asynchronous requests/responses over the same connection are not
 * supported - use a pair of outbound/inbound adapters for that use case.
 * <p>
 * {@link org.springframework.context.Lifecycle} methods delegate to the underlying {@link AbstractConnectionFactory}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.tcp.outbound.TcpOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class TcpOutboundGateway extends org.springframework.integration.ip.tcp.outbound.TcpOutboundGateway {

}
