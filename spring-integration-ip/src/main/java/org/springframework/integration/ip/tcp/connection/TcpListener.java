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

package org.springframework.integration.ip.tcp.connection;

import org.springframework.messaging.Message;

/**
 * Classes that implement this interface may register with a
 * connection factory to receive messages retrieved from a
 * {@link TcpConnection}.
 *
 * @author Gary Russell
 *
 * @since 2.0
 */
@FunctionalInterface
public interface TcpListener {

	/**
	 * Called by a TCPConnection when a new message arrives.
	 * @param message The message.
	 * @return true if the message was intercepted
	 */
	boolean onMessage(Message<?> message);

}
