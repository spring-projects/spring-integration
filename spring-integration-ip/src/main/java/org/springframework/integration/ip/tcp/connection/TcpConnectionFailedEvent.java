/*
 * Copyright 2016-2024 the original author or authors.
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

import org.springframework.integration.ip.event.IpIntegrationEvent;

/**
 * An event emitted when a connection could not be established for some
 * reason.
 *
 * @author Gary Russell
 * @since 4.3.2
 *
 */
public class TcpConnectionFailedEvent extends IpIntegrationEvent {

	private static final long serialVersionUID = -7460880274740273542L;

	public TcpConnectionFailedEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
