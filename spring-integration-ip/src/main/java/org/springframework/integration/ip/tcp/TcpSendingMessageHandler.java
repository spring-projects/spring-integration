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
 * Tcp outbound channel adapter using a TcpConnection to
 * send data - if the connection factory is a server
 * factory, the TcpListener owns the connections. If it is
 * a client factory, this object owns the connection.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.tcp.outbound.TcpSendingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class TcpSendingMessageHandler extends org.springframework.integration.ip.tcp.outbound.TcpSendingMessageHandler {

}
