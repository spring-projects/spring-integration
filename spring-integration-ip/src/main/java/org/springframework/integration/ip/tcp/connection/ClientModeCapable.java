/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Edpoints implementing this interface are capable
 * of running in client-mode. For inbound endpoints,
 * this means that the endpoint establishes the connection
 * and then receives incoming data.
 * <p>
 * For an outbound adapter, it means that the adapter
 * will establish the connection rather than waiting
 * for a message to cause the connection to be
 * established.
 *
 * @author Gary Russell
 * @since 2.1
 *
 */
public interface ClientModeCapable {

	/**
	 * @return true if the endpoint is running in
	 * client mode.
	 */
	@ManagedAttribute
	boolean isClientMode();

	/**
	 * @return true if the endpoint is running in
	 * client mode.
	 */
	@ManagedAttribute
	boolean isClientModeConnected();

	/**
	 * Immediately attempt to establish the connection.
	 */
	@ManagedOperation
	void retryConnection();

}
