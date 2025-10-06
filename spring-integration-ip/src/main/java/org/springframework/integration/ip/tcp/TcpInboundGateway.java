/*
 * Copyright 2002-present the original author or authors.
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

/**
 * Inbound Gateway using a server connection factory - threading is controlled by the
 * factory. For java.net connections, each socket can process only one message at a time.
 * For java.nio connections, messages may be multiplexed but the client will need to
 * provide correlation logic. If the client is a {@link TcpOutboundGateway} multiplexing
 * is not used, but multiple concurrent connections can be used if the connection factory uses
 * single-use connections. For true asynchronous bidirectional communication, a pair of
 * inbound / outbound channel adapters should be used.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.tcp.inbound.TcpInboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class TcpInboundGateway extends org.springframework.integration.ip.tcp.inbound.TcpInboundGateway {

}
